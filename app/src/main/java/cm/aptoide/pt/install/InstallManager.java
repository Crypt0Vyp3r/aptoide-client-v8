/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 04/10/2016.
 */

package cm.aptoide.pt.install;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.database.realm.Installed;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.downloadmanager.DownloadNotFoundException;
import cm.aptoide.pt.downloadmanager.DownloadsRepository;
import cm.aptoide.pt.install.installer.InstallationState;
import cm.aptoide.pt.logger.Logger;
import cm.aptoide.pt.preferences.managed.ManagerPreferences;
import cm.aptoide.pt.preferences.secure.SecurePreferences;
import cm.aptoide.pt.root.RootAvailabilityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

import static cm.aptoide.pt.install.InstallService.ACTION_INSTALL_FINISHED;
import static cm.aptoide.pt.install.InstallService.EXTRA_INSTALLATION_MD5;

/**
 * Created by marcelobenites on 9/29/16.
 */

public class InstallManager {

  private static final String TAG = "InstallManager";
  private final AptoideDownloadManager aptoideDownloadManager;
  private final Installer installer;
  private final SharedPreferences sharedPreferences;
  private final SharedPreferences securePreferences;
  private final Context context;
  private final PackageInstallerManager packageInstallerManager;
  private final DownloadsRepository downloadRepository;
  private final InstalledRepository installedRepository;
  private final RootAvailabilityManager rootAvailabilityManager;
  private final ForegroundManager foregroundManager;

  public InstallManager(Context context, AptoideDownloadManager aptoideDownloadManager,
      Installer installer, RootAvailabilityManager rootAvailabilityManager,
      SharedPreferences sharedPreferences, SharedPreferences securePreferences,
      DownloadsRepository downloadRepository, InstalledRepository installedRepository,
      PackageInstallerManager packageInstallerManager, ForegroundManager foregroundManager) {
    this.aptoideDownloadManager = aptoideDownloadManager;
    this.installer = installer;
    this.context = context;
    this.rootAvailabilityManager = rootAvailabilityManager;
    this.downloadRepository = downloadRepository;
    this.installedRepository = installedRepository;
    this.sharedPreferences = sharedPreferences;
    this.securePreferences = securePreferences;
    this.packageInstallerManager = packageInstallerManager;
    this.foregroundManager = foregroundManager;
  }

  public void start() {
    aptoideDownloadManager.start();
  }

  private void waitForDownloadAndInstall(String md5, boolean forceDefaultInstall,
      boolean forceSplitInstall) {
    aptoideDownloadManager.getDownload(md5)
        .filter(download -> download != null)
        .takeUntil(download -> download.getOverallDownloadStatus() == Download.COMPLETED)
        .filter(download -> download.getOverallDownloadStatus() == Download.COMPLETED)
        .flatMapCompletable(
            download -> stopForegroundAndInstall(download.getMd5(), download.getAction(),
                forceDefaultInstall, forceSplitInstall).andThen(
                sendBackgroundInstallFinishedBroadcast(download)))
        .toCompletable()
        .subscribe(() -> {
        }, Throwable::printStackTrace);
  }

  private Completable sendBackgroundInstallFinishedBroadcast(Download download) {
    return Completable.fromAction(() -> context.sendBroadcast(
        new Intent(ACTION_INSTALL_FINISHED).putExtra(EXTRA_INSTALLATION_MD5, download.getMd5())));
  }

  public void stop() {
    aptoideDownloadManager.stop();
  }

  private Completable stopForegroundAndInstall(String md5, int downloadAction,
      boolean forceDefaultInstall, boolean shouldSetPackageInstaller) {
    Logger.getInstance()
        .d(TAG, "going to pop install from: " + md5 + "and download action: " + downloadAction);
    switch (downloadAction) {
      case Download.ACTION_INSTALL:
        return installer.install(context, md5, forceDefaultInstall, shouldSetPackageInstaller);
      case Download.ACTION_UPDATE:
        return installer.update(context, md5, forceDefaultInstall, shouldSetPackageInstaller);
      case Download.ACTION_DOWNGRADE:
        return installer.downgrade(context, md5, forceDefaultInstall, shouldSetPackageInstaller);
      default:
        return Completable.error(
            new IllegalArgumentException("Invalid download action " + downloadAction));
    }
  }

  public Completable cancelInstall(String md5, String packageName, int versionCode) {
    return pauseInstall(md5).andThen(installedRepository.remove(packageName, versionCode))
        .andThen(aptoideDownloadManager.removeDownload(md5))
        .doOnError(throwable -> throwable.printStackTrace());
  }

  public Completable pauseInstall(String md5) {
    return aptoideDownloadManager.pauseDownload(md5);
  }

  public Observable<List<Install>> getTimedOutInstallations() {
    return getInstallations().flatMap(installs -> Observable.from(installs)
        .filter(install -> install.getState()
            .equals(Install.InstallationStatus.INSTALLATION_TIMEOUT))
        .toList());
  }

  public Observable<List<Install>> getInstalledApps() {
    return installedRepository.getAllInstalled()
        .concatMap(downloadList -> Observable.from(downloadList)
            .flatMap(download -> getInstall(download.getPackageName(),
                download.getVersionCode()).first())
            .toList());
  }

  private Observable<Install> getInstall(String packageName, int versionCode) {
    return installedRepository.get(packageName, versionCode)
        .map(installed -> new Install(100, Install.InstallationStatus.INSTALLED,
            Install.InstallationType.INSTALLED, false, -1, null, installed.getPackageName(),
            installed.getVersionCode(), installed.getVersionName(), installed.getName(),
            installed.getIcon()));
  }

  public Observable<List<Install>> getInstallations() {
    return Observable.combineLatest(aptoideDownloadManager.getDownloadsList(),
        installedRepository.getAllInstalled(), this::createInstallList)
        .distinctUntilChanged();
  }

  private List<Install> createInstallList(List<Download> downloads, List<Installed> installeds) {
    List<Install> installList = new ArrayList<>();
    for (Download download : downloads) {
      boolean found = false;
      for (Installed installed : installeds) {
        if (download.getPackageName()
            .equals(installed.getPackageName())) {
          found = true;
          InstallationState installationState;
          if (download.getVersionCode() == installed.getVersionCode()) {
            installationState =
                new InstallationState(installed.getPackageName(), installed.getVersionCode(),
                    installed.getVersionName(), installed.getStatus(), installed.getType(),
                    installed.getName(), installed.getIcon());
          } else {
            installationState =
                new InstallationState(installed.getPackageName(), installed.getVersionCode(),
                    Installed.STATUS_UNINSTALLED, Installed.TYPE_UNKNOWN);
          }

          Install.InstallationType type;
          if (installed.getVersionCode() == download.getVersionCode()) {
            type = Install.InstallationType.INSTALLED;
          } else if (installed.getVersionCode() > download.getVersionCode()) {
            type = Install.InstallationType.DOWNGRADE;
          } else {
            type = Install.InstallationType.UPDATE;
          }
          installList.add(createInstall(download, installationState, download.getMd5(),
              download.getPackageName(), download.getVersionCode(), type));
          break;
        }
      }

      if (!found) {
        installList.add(createInstall(download,
            new InstallationState(download.getPackageName(), download.getVersionCode(),
                Installed.STATUS_UNINSTALLED, Installed.TYPE_UNKNOWN), download.getMd5(),
            download.getPackageName(), download.getVersionCode(),
            Install.InstallationType.INSTALL));
      }
    }
    return installList;
  }

  public Observable<Install> getCurrentInstallation() {
    return aptoideDownloadManager.getCurrentInProgressDownload()
        .filter(download -> download != null)
        .observeOn(Schedulers.io())
        .distinctUntilChanged(download -> download.getMd5())
        .flatMap(download -> getInstall(download.getMd5(), download.getPackageName(),
            download.getVersionCode()));
  }

  public Completable install(Download download) {
    return install(download, false, false, true);
  }

  public Completable install(Download download, boolean shouldInstall) {
    return install(download, false, false, shouldInstall);
  }

  private Completable defaultInstall(Download download) {
    return install(download, true, false, true);
  }

  public Completable splitInstall(Download download) {
    return install(download, false, true, true);
  }

  private Completable install(Download download, boolean forceDefaultInstall,
      boolean forceSplitInstall, boolean shouldInstall) {
    return aptoideDownloadManager.getDownload(download.getMd5())
        .first()
        .map(storedDownload -> updateDownloadAction(download, storedDownload))
        .retryWhen(errors -> createDownloadAndRetry(errors, download))
        .doOnNext(storedDownload -> {
          if (storedDownload.getOverallDownloadStatus() == Download.ERROR) {
            storedDownload.setOverallDownloadStatus(Download.INVALID_STATUS);
            downloadRepository.save(storedDownload);
          }
        })
        .flatMap(install -> installInBackground(download.getMd5(), forceDefaultInstall,
            packageInstallerManager.shouldSetInstallerPackageName(download) || forceSplitInstall,
            shouldInstall))
        .toCompletable();
  }

  public Observable<Install> getInstall(String md5, String packageName, int versioncode) {
    return Observable.combineLatest(aptoideDownloadManager.getDownloadsByMd5(md5),
        installer.getState(packageName, versioncode), getInstallationType(packageName, versioncode),
        (download, installationState, installationType) -> createInstall(download,
            installationState, md5, packageName, versioncode, installationType))
        .doOnNext(install -> Logger.getInstance()
            .d(TAG, install.toString()));
  }

  private Install createInstall(Download download, InstallationState installationState, String md5,
      String packageName, int versioncode, Install.InstallationType installationType) {
    return new Install(mapInstallation(download),
        mapInstallationStatus(download, installationState), installationType,
        mapIndeterminateState(download, installationState), getSpeed(download), md5, packageName,
        versioncode, getVersionName(download, installationState),
        getAppName(download, installationState), getAppIcon(download, installationState));
  }

  private String getVersionName(Download download, InstallationState installationState) {
    if (download != null) {
      return download.getVersionName();
    } else {
      return installationState.getVersionName();
    }
  }

  private String getAppIcon(Download download, InstallationState installationState) {
    if (download != null) {
      return download.getIcon();
    } else {
      return installationState.getIcon();
    }
  }

  private String getAppName(Download download, InstallationState installationState) {
    if (download != null) {
      return download.getAppName();
    } else {
      return installationState.getName();
    }
  }

  private int getSpeed(Download download) {
    if (download != null) {
      return download.getDownloadSpeed();
    } else {
      return 0;
    }
  }

  private boolean mapIndeterminateState(Download download, InstallationState installationState) {
    return mapIndeterminate(download) || mapInstallIndeterminate(installationState.getStatus(),
        installationState.getType(), download);
  }

  private Install.InstallationStatus mapInstallationStatus(Download download,
      InstallationState installationState) {

    if (installationState.getStatus() == Installed.STATUS_COMPLETED) {
      return Install.InstallationStatus.INSTALLED;
    }

    if (installationState.getStatus() == Installed.STATUS_INSTALLING
        && installationState.getType() != Installed.TYPE_DEFAULT) {
      return Install.InstallationStatus.INSTALLING;
    }

    if (installationState.getStatus() == Installed.STATUS_WAITING
        && download != null
        && download.getOverallDownloadStatus() == Download.COMPLETED) {
      return Install.InstallationStatus.DOWNLOADING;
    }

    if (installationState.getStatus() == Installed.STATUS_ROOT_TIMEOUT) {
      return Install.InstallationStatus.INSTALLATION_TIMEOUT;
    }

    return mapDownloadState(download);
  }

  private int mapInstallation(Download download) {
    int progress = 0;
    if (download != null) {
      progress = download.getOverallProgress();
      Logger.getInstance()
          .d(TAG, " download is not null "
              + progress
              + " state "
              + download.getOverallDownloadStatus());
    } else {
      Logger.getInstance()
          .d(TAG, " download is null");
    }
    return progress;
  }

  private boolean mapIndeterminate(Download download) {
    boolean isIndeterminate = false;
    if (download != null) {
      switch (download.getOverallDownloadStatus()) {
        case Download.IN_QUEUE:
        case Download.WAITING_TO_MOVE_FILES:
          isIndeterminate = true;
          break;
        case Download.BLOCK_COMPLETE:
        case Download.COMPLETED:
        case Download.CONNECTED:
        case Download.ERROR:
        case Download.FILE_MISSING:
        case Download.INVALID_STATUS:
        case Download.NOT_DOWNLOADED:
        case Download.PAUSED:
        case Download.PENDING:
        case Download.PROGRESS:
        case Download.RETRY:
        case Download.STARTED:
        case Download.WARN:
          isIndeterminate = false;
          break;
        default:
          isIndeterminate = false;
      }
    }
    return isIndeterminate;
  }

  private Install.InstallationStatus mapDownloadState(Download download) {
    Install.InstallationStatus status = Install.InstallationStatus.UNINSTALLED;
    if (download != null) {
      switch (download.getOverallDownloadStatus()) {
        case Download.INVALID_STATUS:
          status = Install.InstallationStatus.INITIAL_STATE;
          break;
        case Download.FILE_MISSING:
        case Download.NOT_DOWNLOADED:
        case Download.COMPLETED:
          status = Install.InstallationStatus.UNINSTALLED;
          break;
        case Download.PAUSED:
          status = Install.InstallationStatus.PAUSED;
          break;
        case Download.ERROR:
          switch (download.getDownloadError()) {
            case Download.GENERIC_ERROR:
              status = Install.InstallationStatus.GENERIC_ERROR;
              break;
            case Download.NOT_ENOUGH_SPACE_ERROR:
              status = Install.InstallationStatus.NOT_ENOUGH_SPACE_ERROR;
              break;
          }
          break;
        case Download.RETRY:
        case Download.STARTED:
        case Download.WARN:
        case Download.CONNECTED:
        case Download.BLOCK_COMPLETE:
        case Download.PROGRESS:
        case Download.PENDING:
        case Download.WAITING_TO_MOVE_FILES:
          status = Install.InstallationStatus.DOWNLOADING;
          break;
        case Download.IN_QUEUE:
          status = Install.InstallationStatus.IN_QUEUE;
          break;
      }
    } else {
      Logger.getInstance()
          .d(TAG, "mapping a null Download state");
    }
    return status;
  }

  private boolean mapInstallIndeterminate(int status, int type, Download download) {
    boolean isIndeterminate = false;
    switch (status) {
      case Installed.STATUS_UNINSTALLED:
      case Installed.STATUS_COMPLETED:
        isIndeterminate = false;
        break;
      case Installed.STATUS_INSTALLING:
      case Installed.STATUS_ROOT_TIMEOUT:
        isIndeterminate = type != Installed.TYPE_DEFAULT;
        break;
      case Installed.STATUS_WAITING:
        isIndeterminate =
            download != null && download.getOverallDownloadStatus() == Download.COMPLETED;
    }
    if (download != null && download.getOverallDownloadStatus() == Download.INVALID_STATUS) {
      isIndeterminate = true;
    }
    return isIndeterminate;
  }

  @NonNull private Download updateDownloadAction(Download download, Download storedDownload) {
    if (storedDownload.getAction() != download.getAction()) {
      storedDownload.setAction(download.getAction());
      downloadRepository.save(storedDownload);
    }
    return storedDownload;
  }

  private Observable<Throwable> createDownloadAndRetry(Observable<? extends Throwable> errors,
      Download download) {
    return errors.flatMap(throwable -> {
      if (throwable instanceof DownloadNotFoundException) {
        Logger.getInstance()
            .d(TAG, "saved the newly created download because the other one was null");
        downloadRepository.save(download);
        return Observable.just(throwable);
      } else {
        return Observable.error(throwable);
      }
    });
  }

  private Observable<String> installInBackground(String md5, boolean forceDefaultInstall,
      boolean shouldSetPackageInstaller, boolean shouldInstall) {
    return startBackgroundInstallation(md5, forceDefaultInstall, shouldSetPackageInstaller,
        shouldInstall);
  }

  private Observable<String> startBackgroundInstallation(String md5, boolean forceDefaultInstall,
      boolean shouldSetPackageInstaller, boolean shouldInstall) {
    if (shouldInstall) {
      waitForDownloadAndInstall(md5, forceDefaultInstall, shouldSetPackageInstaller);
    }
    return aptoideDownloadManager.getDownload(md5)
        .first()
        .doOnNext(download -> initInstallationProgress(download))
        .doOnNext(__ -> {
          if (shouldInstall) {
            startInstallService();
          }
        })
        .flatMapCompletable(download -> {
          if (download.getOverallDownloadStatus() == Download.COMPLETED) {
            return Completable.fromAction(() -> {
              Logger.getInstance()
                  .d(TAG,
                      "Saving an already completed download to trigger the download installation");
              downloadRepository.save(download);
            });
          } else {
            return aptoideDownloadManager.startDownload(download);
          }
        })
        .map(__ -> md5);
  }

  private void startInstallService() {
    foregroundManager.startDownloadForeground();
  }

  private void initInstallationProgress(Download download) {
    Installed installed = convertDownloadToInstalled(download);
    installedRepository.save(installed);
  }

  @NonNull private Installed convertDownloadToInstalled(Download download) {
    Installed installed = new Installed();
    installed.setPackageAndVersionCode(download.getPackageName() + download.getVersionCode());
    installed.setVersionCode(download.getVersionCode());
    installed.setVersionName(download.getVersionName());
    installed.setStatus(Installed.STATUS_WAITING);
    installed.setType(Installed.TYPE_UNKNOWN);
    installed.setPackageName(download.getPackageName());
    return installed;
  }

  public boolean showWarning() {
    boolean wasRootDialogShowed = SecurePreferences.isRootDialogShowed(securePreferences);
    boolean isRooted = rootAvailabilityManager.isRootAvailable()
        .toBlocking()
        .value();
    boolean canGiveRoot = ManagerPreferences.allowRootInstallation(securePreferences);
    return isRooted && !wasRootDialogShowed && !canGiveRoot;
  }

  public void rootInstallAllowed(boolean allowRoot) {
    SecurePreferences.setRootDialogShowed(true, securePreferences);
    ManagerPreferences.setAllowRootInstallation(allowRoot, sharedPreferences);
  }

  public Observable<Boolean> startInstalls(List<Download> downloads) {
    return Observable.from(downloads)
        .zipWith(Observable.interval(0, 1, TimeUnit.SECONDS), (download, along) -> download)
        .flatMapCompletable(download -> install(download))
        .toList()
        .map(installs -> true)
        .onErrorReturn(throwable -> false);
  }

  public Completable onAppInstalled(Installed installed) {
    return installedRepository.getAsList(installed.getPackageName())
        .first()
        .flatMapIterable(installeds -> {
          //in case of installation made outside of aptoide
          if (installeds.isEmpty()) {
            installeds.add(installed);
          }
          return installeds;
        })
        .flatMapCompletable(databaseInstalled -> {
          if (databaseInstalled.getVersionCode() == installed.getVersionCode()) {
            installed.setType(databaseInstalled.getType());
            installed.setStatus(Installed.STATUS_COMPLETED);
            return Completable.fromAction(() -> installedRepository.save(installed));
          } else {
            return installedRepository.remove(databaseInstalled.getPackageName(),
                databaseInstalled.getVersionCode());
          }
        })
        .toCompletable();
  }

  public Completable onAppRemoved(String packageName) {
    return installedRepository.getAsList(packageName)
        .first()
        .flatMapIterable(installeds -> installeds)
        .flatMapCompletable(
            installed -> installedRepository.remove(packageName, installed.getVersionCode()))
        .toCompletable();
  }

  private Observable<Install.InstallationType> getInstallationType(String packageName,
      int versionCode) {
    return installedRepository.getInstalled(packageName)
        .map(installed -> {
          if (installed == null) {
            return Install.InstallationType.INSTALL;
          } else if (installed.getVersionCode() == versionCode) {
            return Install.InstallationType.INSTALLED;
          } else if (installed.getVersionCode() > versionCode) {
            return Install.InstallationType.DOWNGRADE;
          } else {
            return Install.InstallationType.UPDATE;
          }
        })
        .doOnNext(installationType -> Logger.getInstance()
            .d("AptoideDownloadManager", " emiting installation type"));
  }

  public Completable onUpdateConfirmed(Installed installed) {
    return onAppInstalled(installed);
  }

  /**
   * The caller is responsible to make sure that the download exists already
   * this method should only be used when a download exists already(ex: resuming)
   *
   * @return the download object to be resumed or null if doesn't exists
   */
  public Single<Download> getDownload(String md5) {
    return downloadRepository.getDownload(md5)
        .first()
        .toSingle();
  }

  public Completable retryTimedOutInstallations() {
    return getTimedOutInstallations().first()
        .flatMapIterable(installs -> installs)
        .flatMapSingle(install -> getDownload(install.getMd5()))
        .flatMapCompletable(download -> defaultInstall(download))
        .toCompletable();
  }

  public Completable cleanTimedOutInstalls() {
    return getTimedOutInstallations().first()
        .flatMap(installs -> Observable.from(installs)
            .flatMap(install -> installedRepository.get(install.getPackageName(),
                install.getVersionCode())
                .first()
                .doOnNext(installed -> {
                  installed.setStatus(Installed.STATUS_UNINSTALLED);
                  installedRepository.save(installed);
                })))
        .toList()
        .toCompletable();
  }

  public Observable<List<Installed>> fetchInstalled() {
    return installedRepository.getAllInstalledSorted()
        .first()
        .flatMapIterable(list -> list)
        .filter(item -> !item.isSystemApp())
        .toList();
  }

  public Observable<Boolean> isInstalled(String packageName) {
    return installedRepository.isInstalled(packageName)
        .first();
  }

  public Observable<Install> filterInstalled(Install item) {
    return installedRepository.isInstalled(item.getPackageName())
        .first()
        .flatMap(isInstalled -> {
          if (isInstalled) {
            return Observable.empty();
          }
          return Observable.just(item);
        });
  }

  public boolean wasAppEverInstalled(String packageName) {
    return installedRepository.getInstallationsHistory()
        .first()
        .flatMapIterable(installation -> installation)
        .filter(installation -> packageName.equals(installation.getPackageName()))
        .toList()
        .flatMap(installations -> {
          if (installations.isEmpty()) {
            return Observable.just(Boolean.FALSE);
          } else {
            return Observable.just(Boolean.TRUE);
          }
        })
        .toBlocking()
        .first();
  }

  public Observable<Install.InstallationStatus> getDownloadState(String md5) {
    return aptoideDownloadManager.getDownload(md5)
        .first()
        .map(download -> mapDownloadState(download));
  }

  public Single<Boolean> hasNextDownload() {
    return aptoideDownloadManager.getCurrentActiveDownloads()
        .first()
        .map(downloads -> downloads != null && !downloads.isEmpty())
        .toSingle();
  }
}
