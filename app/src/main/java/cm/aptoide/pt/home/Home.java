package cm.aptoide.pt.home;

import cm.aptoide.pt.dataprovider.model.v7.Event;
import cm.aptoide.pt.impressions.ImpressionManager;
import java.util.ArrayList;
import java.util.List;
import rx.Completable;
import rx.Single;

/**
 * Created by jdandrade on 07/03/2018.
 */

public class Home {

  private final BundlesRepository bundlesRepository;
  private final ImpressionManager impressionManager;

  public Home(BundlesRepository bundlesRepository, ImpressionManager impressionManager) {
    this.bundlesRepository = bundlesRepository;
    this.impressionManager = impressionManager;
  }

  public Single<HomeBundlesModel> loadHomeBundles() {
    return bundlesRepository.loadHomeBundles()
        .map(homeBundlesModel -> {
          if (homeBundlesModel.hasErrors()) {
            return new HomeBundlesModel(homeBundlesModel.getError());
          } else {
            return new HomeBundlesModel(injectLargeBanner(homeBundlesModel.getList()),
                homeBundlesModel.isLoading(), homeBundlesModel.getOffset());
          }
        });
  }

  private List<HomeBundle> injectLargeBanner(List<HomeBundle> list) {
    list.add(1, new HomeBundle() {
      @Override public String getTitle() {
        return "Advertising";
      }

      @Override public List<?> getContent() {
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(new BannerAd());
        return objects;
      }

      @Override public BundleType getType() {
        return BundleType.BANNER;
      }

      @Override public Event getEvent() {
        return null;
      }

      @Override public String getTag() {
        return null;
      }
    });
    return list;
  }

  public Single<HomeBundlesModel> loadFreshHomeBundles() {
    return bundlesRepository.loadFreshHomeBundles()
        .map(homeBundlesModel -> {
          if (homeBundlesModel.hasErrors()) {
            return new HomeBundlesModel(homeBundlesModel.getError());
          } else {
            return new HomeBundlesModel(injectMediumBanner(homeBundlesModel.getList()),
                homeBundlesModel.isLoading(), homeBundlesModel.getOffset());
          }
        });
  }

  private List<HomeBundle> injectMediumBanner(List<HomeBundle> list) {
    list.add(1, new HomeBundle() {
      @Override public String getTitle() {
        return "Advertising";
      }

      @Override public List<?> getContent() {
        ArrayList<Object> objects = new ArrayList<>();
        objects.add(new BannerAd());
        return objects;
      }

      @Override public BundleType getType() {
        return BundleType.LARGE_BANNER;
      }

      @Override public Event getEvent() {
        return null;
      }

      @Override public String getTag() {
        return null;
      }
    });
    return list;
  }

  public Single<HomeBundlesModel> loadNextHomeBundles() {
    return bundlesRepository.loadNextHomeBundles();
  }

  public boolean hasMore() {
    return bundlesRepository.hasMore();
  }

  public Completable remove(ActionBundle bundle) {
    return impressionManager.markAsRead(bundle.getActionItem()
        .getCardId(), true)
        .andThen(bundlesRepository.remove(bundle));
  }

  public Completable actionBundleImpression(ActionBundle bundle) {
    return impressionManager.markAsRead(bundle.getActionItem()
        .getCardId(), false);
  }
}
