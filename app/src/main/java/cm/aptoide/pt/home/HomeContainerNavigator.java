package cm.aptoide.pt.home;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import cm.aptoide.pt.R;
import cm.aptoide.pt.app.view.MoreBundleFragment;
import cm.aptoide.pt.navigator.FragmentNavigator;
import cm.aptoide.pt.store.view.StoreTabGridRecyclerFragment;
import cm.aptoide.pt.themes.DarkThemeDialogFragment;
import rx.Observable;

public class HomeContainerNavigator {

  private FragmentNavigator childFragmentNavigator;
  private String homeTag;
  private String gamesTag;
  private String appsTag;

  public HomeContainerNavigator(FragmentNavigator childFragmentNavigator) {
    this.childFragmentNavigator = childFragmentNavigator;
  }

  public void showDarkThemeDialog() {
    DarkThemeDialogFragment darkThemeDialogFragment = new DarkThemeDialogFragment();
    childFragmentNavigator.navigateToDialogFragment(darkThemeDialogFragment);
  }

  public void loadMainHomeContent() {
    Fragment fragment = childFragmentNavigator.getFragment(homeTag);
    if (fragment != null) {
      childFragmentNavigator.navigateToWithoutBackSave(fragment, true);
    } else {
      homeTag = childFragmentNavigator.navigateTo(new HomeFragment(), true);
    }
  }

  public void loadGamesHomeContent() {
    Fragment fragment = new MoreBundleFragment();
    Bundle args = new Bundle();
    args.putString(StoreTabGridRecyclerFragment.BundleCons.TITLE,
        childFragmentNavigator.getFragment()
            .getString(R.string.home_chip_games));
    args.putString(StoreTabGridRecyclerFragment.BundleCons.ACTION,
        "https://ws75.aptoide.com/api/7/getStoreWidgets/store_id=15/context=games/widget=apps_list%3A0%262%3Adownloads7d");
    args.putBoolean(StoreTabGridRecyclerFragment.BundleCons.TOOLBAR, false);
    fragment.setArguments(args);

    Fragment gamesFragment = childFragmentNavigator.getFragment(gamesTag);
    if (gamesFragment != null) {
      childFragmentNavigator.navigateToWithoutBackSave(gamesFragment, true);
    } else {
      gamesTag = childFragmentNavigator.navigateTo(fragment, true);
    }
  }

  public void loadAppsHomeContent() {
    Fragment fragment = new MoreBundleFragment();
    Bundle args = new Bundle();
    args.putString(StoreTabGridRecyclerFragment.BundleCons.TITLE,
        childFragmentNavigator.getFragment()
            .getString(R.string.home_chip_apps));
    args.putString(StoreTabGridRecyclerFragment.BundleCons.ACTION,
        "https://ws75.aptoide.com/api/7/getStoreWidgets/store_id=15/context=apps/widget=apps_list%3A0%261%3Apdownloads7d");
    args.putBoolean(StoreTabGridRecyclerFragment.BundleCons.TOOLBAR, false);
    fragment.setArguments(args);

    Fragment appsFragment = childFragmentNavigator.getFragment(appsTag);
    if (appsFragment != null) {
      childFragmentNavigator.navigateToWithoutBackSave(appsFragment, true);
    } else {
      appsTag = childFragmentNavigator.navigateTo(fragment, true);
    }
  }

  public Observable<Boolean> navigateHome() {
    Fragment fragment = childFragmentNavigator.getFragment();
    if (fragment instanceof ScrollableView) {
      ScrollableView view = (ScrollableView) fragment;
      if (view.isAtTop()) {
        if (fragment instanceof MoreBundleFragment) {
          return Observable.just(true);
        }
      }
      view.scrollToTop();
    }
    return Observable.just(false);
  }
}
