/*
 * Copyright (c) 2016.
 * Modified by Marcelo Benites on 25/07/2016.
 */

package cm.aptoide.pt.v8engine.install.provider;

import cm.aptoide.pt.database.accessors.DownloadAccessor;
import cm.aptoide.pt.database.realm.Download;
import cm.aptoide.pt.downloadmanager.AptoideDownloadManager;
import cm.aptoide.pt.v8engine.install.exception.InstallationException;
import cm.aptoide.pt.v8engine.install.installer.InstallationProvider;
import cm.aptoide.pt.v8engine.install.installer.RollbackInstallation;
import rx.Observable;

/**
 * Created by marcelobenites on 7/25/16.
 */
public class DownloadInstallationProvider implements InstallationProvider {

  private final AptoideDownloadManager downloadManager;
  private final DownloadAccessor downloadAccessor;

  public DownloadInstallationProvider(AptoideDownloadManager downloadManager,
      DownloadAccessor downloadAccessor) {
    this.downloadManager = downloadManager;
    this.downloadAccessor = downloadAccessor;
  }

  @Override public Observable<RollbackInstallation> getInstallation(String md5) {
    return downloadManager.getDownload(md5).first().flatMap(download -> {
      if (download.getOverallDownloadStatus() == Download.COMPLETED) {
        return Observable.just(new DownloadInstallationAdapter(download, downloadAccessor));
      }
      return Observable.error(new InstallationException("Installation file not available."));
    });
  }
}
