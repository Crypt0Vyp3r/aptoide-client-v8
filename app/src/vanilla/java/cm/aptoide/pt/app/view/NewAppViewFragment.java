package cm.aptoide.pt.app.view;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import cm.aptoide.pt.R;
import cm.aptoide.pt.analytics.ScreenTagHistory;
import cm.aptoide.pt.app.AppViewSimilarApp;
import cm.aptoide.pt.app.DetailedAppViewModel;
import cm.aptoide.pt.app.DownloadAppViewModel;
import cm.aptoide.pt.app.ReviewsViewModel;
import cm.aptoide.pt.app.SimilarAppsViewModel;
import cm.aptoide.pt.app.view.screenshots.NewScreenshotsAdapter;
import cm.aptoide.pt.app.view.screenshots.ScreenShotClickEvent;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.dataprovider.model.v7.GetAppMeta;
import cm.aptoide.pt.dataprovider.model.v7.Malware;
import cm.aptoide.pt.dataprovider.model.v7.Review;
import cm.aptoide.pt.dataprovider.ws.v7.store.StoreContext;
import cm.aptoide.pt.networking.image.ImageLoader;
import cm.aptoide.pt.permission.DialogPermissions;
import cm.aptoide.pt.reviews.LanguageFilterHelper;
import cm.aptoide.pt.store.StoreTheme;
import cm.aptoide.pt.utils.AptoideUtils;
import cm.aptoide.pt.utils.GenericDialogs;
import cm.aptoide.pt.utils.design.ShowMessage;
import cm.aptoide.pt.view.app.Application;
import cm.aptoide.pt.view.app.DetailedApp;
import cm.aptoide.pt.view.dialog.DialogBadgeV7;
import cm.aptoide.pt.view.dialog.DialogUtils;
import cm.aptoide.pt.view.fragment.NavigationTrackFragment;
import cm.aptoide.pt.view.recycler.LinearLayoutManagerWithSmoothScroller;
import com.jakewharton.rxbinding.view.RxView;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import rx.Observable;
import rx.subjects.PublishSubject;

import static cm.aptoide.pt.utils.GenericDialogs.EResponse.YES;

/**
 * Created by franciscocalado on 07/05/18.
 */

public class NewAppViewFragment extends NavigationTrackFragment implements AppViewView {
  private static final String ORIGIN_TAG = "TAG";
  private static final String BADGE_DIALOG_TAG = "badgeDialog";

  @Inject AppViewPresenter presenter;
  @Inject DialogUtils dialogUtils;
  private Menu menu;
  private Toolbar toolbar;
  private ActionBar actionBar;
  private long appId;
  private String packageName;
  private NewScreenshotsAdapter screenshotsAdapter;
  private TopReviewsAdapter reviewsAdapter;
  private AppViewSimilarAppsAdapter similarAppsAdapter;
  private AppViewSimilarAppsAdapter similarDownloadsAdapter;
  private PublishSubject<ScreenShotClickEvent> screenShotClick;
  private PublishSubject<ReadMoreClickEvent> readMoreClick;
  private PublishSubject<Void> loginSnackClick;
  private PublishSubject<SimilarAppClickEvent> similarAppClick;

  //Views
  private ImageView appIcon;
  private TextView appName;
  private View trustedLayout;
  private ImageView trustedBadge;
  private TextView trustedText;
  private TextView downloadsTop;
  private TextView sizeInfo;
  private TextView appcValue;
  private View similarDownloadView;
  private RecyclerView similarDownloadApps;
  private TextView latestVersion;
  private TextView otherVersions;
  private RecyclerView screenshots;
  private TextView descriptionText;
  private Button descriptionReadMore;
  private ContentLoadingProgressBar topReviewsProgress;
  private View ratingLayout;
  private View emptyReviewsLayout;
  private View commentsLayout;
  private Button rateAppButtonLarge;
  private TextView emptyReviewTextView;
  private TextView reviewUsers;
  private TextView avgReviewScore;
  private RatingBar avgReviewScoreBar;
  private RecyclerView commentsView;
  private Button rateAppButton;
  private Button showAllCommentsButton;

  private View goodAppLayoutWrapper;
  private View flagsLayoutWrapper;
  private View workingWellLayout;
  private View needsLicenseLayout;
  private View fakeAppLayout;
  private View virusLayout;
  private TextView workingWellText;
  private TextView needsLicenceText;
  private TextView fakeAppText;
  private TextView virusText;
  private View storeLayout;
  private ImageView storeIcon;
  private TextView storeName;
  private TextView storeFollowers;
  private TextView storeDownloads;
  private Button storeFollow;
  private View similarBottomView;
  private RecyclerView similarApps;
  private TextView infoWebsite;
  private TextView infoEmail;
  private TextView infoPrivacy;
  private TextView infoPermissions;

  private ProgressBar viewProgress;
  private View appview;

  private Button install;
  private LinearLayout downloadInfoLayout;
  private ProgressBar downloadProgressBar;
  private ImageView cancelDownload;
  private ImageView pauseDownload;
  private ImageView resumeDownload;
  private DownloadAppViewModel.Action action;

  public static NewAppViewFragment newInstance(long appId, String packageName,
      AppViewFragment.OpenType openType, String tag) {
    Bundle bundle = new Bundle();
    bundle.putString(ORIGIN_TAG, tag);
    bundle.putLong(NewAppViewFragment.BundleKeys.APP_ID.name(), appId);
    bundle.putString(NewAppViewFragment.BundleKeys.PACKAGE_NAME.name(), packageName);
    bundle.putSerializable(NewAppViewFragment.BundleKeys.SHOULD_INSTALL.name(), openType);
    NewAppViewFragment fragment = new NewAppViewFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getFragmentComponent(savedInstanceState).inject(this);
    screenShotClick = PublishSubject.create();
    readMoreClick = PublishSubject.create();
    loginSnackClick = PublishSubject.create();
    similarAppClick = PublishSubject.create();
    setHasOptionsMenu(true);
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    if (savedInstanceState != null) {
      appId = savedInstanceState.getLong(NewAppViewFragment.BundleKeys.APP_ID.name(), -1);
      packageName =
          savedInstanceState.getString(NewAppViewFragment.BundleKeys.PACKAGE_NAME.name(), null);
    }

    appIcon = (ImageView) view.findViewById(R.id.app_icon);
    trustedBadge = (ImageView) view.findViewById(R.id.trusted_badge);
    appName = (TextView) view.findViewById(R.id.app_name);
    trustedLayout = view.findViewById(R.id.trusted_layout);
    trustedText = (TextView) view.findViewById(R.id.trusted_text);
    downloadsTop = (TextView) view.findViewById(R.id.header_downloads);
    sizeInfo = (TextView) view.findViewById(R.id.header_size);
    appcValue = (TextView) view.findViewById(R.id.appc_layout)
        .findViewById(R.id.appcoins_reward_message);
    similarDownloadView = view.findViewById(R.id.similar_download_apps);
    similarDownloadApps = (RecyclerView) similarDownloadView.findViewById(R.id.similar_list);
    latestVersion = (TextView) view.findViewById(R.id.versions_layout)
        .findViewById(R.id.latest_version);
    otherVersions = (TextView) view.findViewById(R.id.other_versions);

    screenshots = (RecyclerView) view.findViewById(R.id.screenshots_list);
    screenshots.setLayoutManager(
        new LinearLayoutManager(view.getContext(), LinearLayoutManager.HORIZONTAL, false));
    screenshots.setNestedScrollingEnabled(false);

    descriptionText = (TextView) view.findViewById(R.id.description_text);
    descriptionReadMore = (Button) view.findViewById(R.id.description_see_more);
    topReviewsProgress = (ContentLoadingProgressBar) view.findViewById(R.id.top_comments_progress);
    ratingLayout = view.findViewById(R.id.rating_layout);
    emptyReviewsLayout = view.findViewById(R.id.empty_reviews_layout);
    commentsLayout = view.findViewById(R.id.comments_layout);
    rateAppButtonLarge = (Button) view.findViewById(R.id.rate_this_button2);
    emptyReviewTextView = (TextView) view.findViewById(R.id.empty_review_text);
    reviewUsers = (TextView) view.findViewById(R.id.users_voted);
    avgReviewScore = (TextView) view.findViewById(R.id.rating_value);
    avgReviewScoreBar = (RatingBar) view.findViewById(R.id.rating_bar);
    commentsView = (RecyclerView) view.findViewById(R.id.top_comments_list);
    rateAppButton = (Button) view.findViewById(R.id.rate_this_button);
    showAllCommentsButton = (Button) view.findViewById(R.id.read_all_button);

    goodAppLayoutWrapper = view.findViewById(R.id.good_app_layout);
    flagsLayoutWrapper = view.findViewById(R.id.rating_flags_layout);
    workingWellLayout = view.findViewById(R.id.working_well_layout);
    needsLicenseLayout = view.findViewById(R.id.needs_licence_layout);
    fakeAppLayout = view.findViewById(R.id.fake_app_layout);
    virusLayout = view.findViewById(R.id.virus_layout);

    workingWellText = (TextView) view.findViewById(R.id.working_well_count);
    needsLicenceText = (TextView) view.findViewById(R.id.needs_licence_count);
    fakeAppText = (TextView) view.findViewById(R.id.fake_app_count);
    virusText = (TextView) view.findViewById(R.id.virus_count);
    storeLayout = view.findViewById(R.id.store_uploaded_layout);
    storeIcon = (ImageView) view.findViewById(R.id.store_icon);
    storeName = (TextView) view.findViewById(R.id.store_name);
    storeFollowers = (TextView) view.findViewById(R.id.user_count);
    storeDownloads = (TextView) view.findViewById(R.id.download_count);
    storeFollow = (Button) view.findViewById(R.id.follow_button);
    similarBottomView = view.findViewById(R.id.similar_layout);
    similarApps = (RecyclerView) similarBottomView.findViewById(R.id.similar_list);
    infoWebsite = (TextView) view.findViewById(R.id.website_label);
    infoEmail = (TextView) view.findViewById(R.id.email_label);
    infoPrivacy = (TextView) view.findViewById(R.id.privacy_policy_label);
    infoPermissions = (TextView) view.findViewById(R.id.permissions_label);

    viewProgress = (ProgressBar) view.findViewById(R.id.appview_progress);
    appview = view.findViewById(R.id.appview_full);
    toolbar = (Toolbar) view.findViewById(R.id.toolbar);

    install = ((Button) view.findViewById(R.id.appview_install_button));
    downloadInfoLayout = ((LinearLayout) view.findViewById(R.id.appview_transfer_info));
    downloadProgressBar = ((ProgressBar) view.findViewById(R.id.appview_download_progress_bar));
    cancelDownload = ((ImageView) view.findViewById(R.id.appview_download_cancel_button));
    resumeDownload = ((ImageView) view.findViewById(R.id.appview_download_resume_download));
    pauseDownload = ((ImageView) view.findViewById(R.id.appview_download_pause_download));

    screenshotsAdapter =
        new NewScreenshotsAdapter(new ArrayList<>(), new ArrayList<>(), screenShotClick);
    screenshots.setAdapter(screenshotsAdapter);

    LinearLayoutManagerWithSmoothScroller layoutManager =
        new LinearLayoutManagerWithSmoothScroller(getContext(), LinearLayoutManager.HORIZONTAL,
            false);
    LinearLayoutManager similarLayout =
        new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
    LinearLayoutManager similarDownloadsLayout =
        new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);

    commentsView.setLayoutManager(layoutManager);
    // because otherwise the AppBar won't be collapsed
    commentsView.setNestedScrollingEnabled(false);
    similarApps.setNestedScrollingEnabled(false);

    similarAppsAdapter =
        new AppViewSimilarAppsAdapter(Collections.emptyList(), new DecimalFormat("#,#"),
            similarAppClick);
    similarDownloadsAdapter =
        new AppViewSimilarAppsAdapter(Collections.emptyList(), new DecimalFormat("#,#"),
            similarAppClick);

    similarDownloadApps.setAdapter(similarDownloadsAdapter);
    similarApps.setAdapter(similarAppsAdapter);
    similarApps.setLayoutManager(similarLayout);

    similarApps.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
          RecyclerView.State state) {
        int margin = AptoideUtils.ScreenU.getPixelsForDip(5, view.getResources());
        outRect.set(margin, margin, 0, margin);
        similarDownloadApps.setLayoutManager(similarDownloadsLayout);
      }
    });

    similarDownloadApps.addItemDecoration(new RecyclerView.ItemDecoration() {
      @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
          RecyclerView.State state) {
        int margin = AptoideUtils.ScreenU.getPixelsForDip(5, view.getResources());
        outRect.set(margin, margin, 0, margin);
      }
    });

    setupToolbar();
    attachPresenter(presenter);
  }

  @Override public ScreenTagHistory getHistoryTracker() {
    return ScreenTagHistory.Builder.build("AppViewFragment", "", StoreContext.meta);
  }

  @Override public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);
    return inflater.inflate(R.layout.fragment_new_app_view, container, false);
  }

  @Override public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    this.menu = menu;
    inflater.inflate(R.menu.fragment_appview, menu);
    showHideOptionsMenu(true);
  }

  @Override public void showLoading() {
    appview.setVisibility(View.GONE);
    viewProgress.setVisibility(View.VISIBLE);
  }

  @Override public void showAppview() {
    appview.setVisibility(View.VISIBLE);
    viewProgress.setVisibility(View.GONE);
  }

  @Override public long getAppId() {
    return appId;
  }

  @Override public String getPackageName() {
    return packageName;
  }

  @Override public void populateAppDetails(DetailedAppViewModel detailedApp) {
    StoreTheme storeThemeEnum = StoreTheme.get(detailedApp.getDetailedApp()
        .getStore());

    appName.setText(detailedApp.getDetailedApp()
        .getName());
    ImageLoader.with(getContext())
        .load(detailedApp.getDetailedApp()
            .getIcon(), appIcon);
    downloadsTop.setText(String.format("%s", AptoideUtils.StringU.withSuffix(
        detailedApp.getDetailedApp()
            .getStats()
            .getPdownloads())));
    sizeInfo.setText(AptoideUtils.StringU.formatBytes(detailedApp.getDetailedApp()
        .getSize(), false));
    latestVersion.setText(detailedApp.getDetailedApp()
        .getFile()
        .getVername());
    storeName.setText(detailedApp.getDetailedApp()
        .getStore()
        .getName());
    ImageLoader.with(getContext())
        .loadWithShadowCircleTransform(detailedApp.getDetailedApp()
            .getStore()
            .getAvatar(), storeIcon);
    storeDownloads.setText(String.format("%s", AptoideUtils.StringU.withSuffix(
        detailedApp.getDetailedApp()
            .getStore()
            .getStats()
            .getDownloads())));
    storeFollowers.setText(String.format("%s", AptoideUtils.StringU.withSuffix(
        detailedApp.getDetailedApp()
            .getStore()
            .getStats()
            .getSubscribers())));
    storeFollow.setBackgroundDrawable(
        storeThemeEnum.getButtonLayoutDrawable(getResources(), getContext().getTheme()));
    if (detailedApp.isStoreFollowed()) {
      storeFollow.setText(R.string.followed);
    } else {
      storeFollow.setText(R.string.follow);
    }
    if ((detailedApp.getDetailedApp()
        .getMedia()
        .getScreenshots() != null && !detailedApp.getDetailedApp()
        .getMedia()
        .getScreenshots()
        .isEmpty()) || (detailedApp.getDetailedApp()
        .getMedia()
        .getVideos() != null && !detailedApp.getDetailedApp()
        .getMedia()
        .getVideos()
        .isEmpty())) {
      screenshotsAdapter.updateScreenshots(detailedApp.getDetailedApp()
          .getMedia()
          .getScreenshots());
      screenshotsAdapter.updateVideos(detailedApp.getDetailedApp()
          .getMedia()
          .getVideos());
    } else {
      screenshots.setVisibility(View.GONE);
    }
    setTrustedBadge(detailedApp.getDetailedApp());
    setDescription(detailedApp.getDetailedApp()
        .getMedia()
        .getDescription());
    setAppFlags(detailedApp.getDetailedApp()
        .getFile());
    setReadMoreClickListener(detailedApp.getDetailedApp());
    setDeveloperDetails(detailedApp.getDetailedApp());
    showAppview();
  }

  @Override public Observable<ScreenShotClickEvent> getScreenshotClickEvent() {
    return screenShotClick;
  }

  @Override public Observable<ReadMoreClickEvent> clickedReadMore() {
    return readMoreClick;
  }

  @Override
  public Void populateReviewsAndAds(ReviewsViewModel reviewsModel, SimilarAppsViewModel ads,
      DetailedApp app) {
    List<Review> reviews = reviewsModel.getReviewsList();

    if (reviews != null && !reviews.isEmpty()) {
      showReviews(true, app);

      reviewsAdapter = new TopReviewsAdapter(reviews.toArray(new Review[reviews.size()]));
    } else {
      showReviews(false, app);
      reviewsAdapter = new TopReviewsAdapter();
    }

    commentsView.setAdapter(reviewsAdapter);
    similarAppsAdapter.update(mapToSimilar(ads));
    similarDownloadsAdapter.update(mapToSimilar(ads));

    return null;
  }

  @Override public Observable<GetAppMeta.GetAppMetaFile.Flags.Vote.Type> clickWorkingFlag() {
    return RxView.clicks(workingWellLayout)
        .flatMap(__ -> Observable.just(GetAppMeta.GetAppMetaFile.Flags.Vote.Type.GOOD));
  }

  @Override public Observable<GetAppMeta.GetAppMetaFile.Flags.Vote.Type> clickLicenseFlag() {
    return RxView.clicks(needsLicenseLayout)
        .flatMap(__ -> Observable.just(GetAppMeta.GetAppMetaFile.Flags.Vote.Type.LICENSE));
  }

  @Override public Observable<GetAppMeta.GetAppMetaFile.Flags.Vote.Type> clickFakeFlag() {
    return RxView.clicks(fakeAppLayout)
        .flatMap(__ -> Observable.just(GetAppMeta.GetAppMetaFile.Flags.Vote.Type.FAKE));
  }

  @Override public Observable<GetAppMeta.GetAppMetaFile.Flags.Vote.Type> clickVirusFlag() {
    return RxView.clicks(virusLayout)
        .flatMap(__ -> Observable.just(GetAppMeta.GetAppMetaFile.Flags.Vote.Type.VIRUS));
  }

  @Override public void displayNotLoggedInSnack() {
    //Toast.makeText(getContext(), R.string.you_need_to_be_logged_in, Toast.LENGTH_SHORT)
    //    .show();
    ShowMessage.asSnack(getView(), R.string.you_need_to_be_logged_in, R.string.login, snackView -> {
      loginSnackClick.onNext(null);
    });
  }

  @Override public void displayStoreFollowedSnack(String storeName) {
    String messageToDisplay = String.format(getString(R.string.store_followed), storeName);
    Toast.makeText(getContext(), messageToDisplay, Toast.LENGTH_SHORT)
        .show();
  }

  @Override public Observable<Void> clickDeveloperWebsite() {
    return RxView.clicks(infoWebsite);
  }

  @Override public Observable<Void> clickDeveloperEmail() {
    return RxView.clicks(infoEmail);
  }

  @Override public Observable<Void> clickDeveloperPrivacy() {
    return RxView.clicks(infoPrivacy);
  }

  @Override public Observable<Void> clickDeveloperPermissions() {
    return RxView.clicks(infoPermissions);
  }

  @Override public Observable<Void> clickStoreLayout() {
    return RxView.clicks(storeLayout);
  }

  @Override public Observable<Void> clickFollowStore() {
    return RxView.clicks(storeFollow);
  }

  @Override public Observable<Void> clickOtherVersions() {
    return RxView.clicks(otherVersions);
  }

  @Override public Observable<Void> clickTrustedBadge() {
    return RxView.clicks(trustedLayout);
  }

  @Override public Observable<Void> clickRateApp() {
    return RxView.clicks(rateAppButton);
  }

  @Override public Observable<Void> clickRateAppLarge() {
    return RxView.clicks(rateAppButtonLarge);
  }

  @Override public Observable<Void> clickRateAppLayout() {
    return RxView.clicks(ratingLayout);
  }

  @Override public Observable<Void> clickCommentsLayout() {
    return RxView.clicks(commentsLayout);
  }

  @Override public Observable<Void> clickReadAllComments() {
    return RxView.clicks(showAllCommentsButton);
  }

  @Override public Observable<Void> clickLoginSnack() {
    return loginSnackClick;
  }

  @Override public Observable<SimilarAppClickEvent> clickSimilarApp() {
    return similarAppClick;
  }

  @Override public void navigateToDeveloperWebsite(DetailedApp app) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.getDeveloper()
        .getWebsite()));
    getContext().startActivity(browserIntent);
  }

  @Override public void navigateToDeveloperEmail(DetailedApp app) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    Uri data = Uri.parse("mailto:" + app.getDeveloper()
        .getEmail() + "?subject=" + "Feedback" + "&body=" + "");
    intent.setData(data);
    getContext().startActivity(intent);
  }

  @Override public void navigateToDeveloperPrivacy(DetailedApp app) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(app.getDeveloper()
        .getPrivacy()));
    getContext().startActivity(browserIntent);
  }

  @Override public void navigateToDeveloperPermissions(DetailedApp app) {
    DialogPermissions dialogPermissions = DialogPermissions.newInstance(app);
    dialogPermissions.show(getActivity().getSupportFragmentManager(), "");
  }

  @Override public void setFollowButton(boolean isFollowing) {
    if (!isFollowing) storeFollow.setText(R.string.followed);
  }

  @Override public void showTrustedDialog(DetailedApp app) {
    DialogBadgeV7.newInstance(app.getFile()
        .getMalware(), app.getName(), app.getFile()
        .getMalware()
        .getRank())
        .show(getFragmentManager(), BADGE_DIALOG_TAG);
  }

  @Override public String getLanguageFilter() {
    List<String> countryCodes =
        new LanguageFilterHelper(getContext().getResources()).getCurrentLanguageFirst()
            .getCountryCodes();
    return countryCodes.get(0);
  }

  @Override
  public Observable<GenericDialogs.EResponse> showRateDialog(String appName, String packageName,
      String storeName) {
    return dialogUtils.showRateDialog(getActivity(), appName, packageName, storeName);
  }

  @Override public void disableFlags() {
    workingWellLayout.setClickable(false);
    needsLicenseLayout.setClickable(false);
    fakeAppLayout.setClickable(false);
    virusLayout.setClickable(false);
  }

  @Override public void enableFlags() {
    workingWellLayout.setClickable(true);
    needsLicenseLayout.setClickable(true);
    fakeAppLayout.setClickable(true);
    virusLayout.setClickable(true);
  }

  @Override public void incrementFlags(GetAppMeta.GetAppMetaFile.Flags.Vote.Type type) {
    disableFlags();
    switch (type) {
      case GOOD:
        workingWellText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(String.valueOf(new BigDecimal(workingWellText.getText()
                .toString()))) + 1));
        workingWellLayout.setSelected(true);
        workingWellLayout.setPressed(false);
        break;

      case LICENSE:
        needsLicenceText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(String.valueOf(new BigDecimal(needsLicenceText.getText()
                .toString()))) + 1));
        needsLicenceText.setSelected(true);
        needsLicenceText.setPressed(false);

        break;

      case FAKE:
        fakeAppText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(String.valueOf(new BigDecimal(fakeAppText.getText()
                .toString()))) + 1));
        fakeAppLayout.setSelected(true);
        fakeAppLayout.setPressed(false);
        break;

      case VIRUS:
        virusText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(String.valueOf(new BigDecimal(virusText.getText()
                .toString()))) + 1));
        virusLayout.setSelected(true);
        virusLayout.setPressed(false);
        break;

      default:
        throw new IllegalArgumentException("Unable to find Type " + type.name());
    }
  }

  @Override public void showFlagVoteSubmittedMessage() {
    Toast.makeText(getContext(), R.string.vote_submitted, Toast.LENGTH_SHORT)
        .show();
  }

  private void setTrustedBadge(DetailedApp app) {
    @DrawableRes int badgeResId;
    @StringRes int badgeMessageId;

    Malware.Rank rank = app.getFile()
        .getMalware()
        .getRank() == null ? Malware.Rank.UNKNOWN : app.getFile()
        .getMalware()
        .getRank();
    switch (rank) {
      case TRUSTED:
        badgeResId = R.drawable.ic_badge_trusted;
        badgeMessageId = R.string.appview_header_trusted_text;
        break;

      case WARNING:
        badgeResId = R.drawable.ic_badge_warning;
        badgeMessageId = R.string.warning;
        break;

      case CRITICAL:
        badgeResId = R.drawable.ic_badge_critical;
        badgeMessageId = R.string.critical;
        break;

      default:
      case UNKNOWN:
        badgeResId = R.drawable.ic_badge_unknown;
        badgeMessageId = R.string.unknown;
        break;
    }
    Drawable icon = ContextCompat.getDrawable(getContext(), badgeResId);
    trustedBadge.setImageDrawable(icon);
    trustedText.setText(badgeMessageId);
  }

  private void setDescription(String description) {
    if (!TextUtils.isEmpty(description)) {
      descriptionText.setText(AptoideUtils.HtmlU.parse(description));
    } else {
      // only show "default" description if the app doesn't have one
      descriptionText.setText(R.string.description_not_available);
      descriptionReadMore.setVisibility(View.GONE);
    }
  }

  private void setReadMoreClickListener(DetailedApp detailedApp) {
    descriptionReadMore.setOnClickListener(view -> readMoreClick.onNext(
        new ReadMoreClickEvent(detailedApp.getName(), detailedApp.getMedia()
            .getDescription(), detailedApp.getStore()
            .getAppearance()
            .getTheme())));
  }

  private void setAppFlags(GetAppMeta.GetAppMetaFile file) {
    if (file.isGoodApp()) {
      goodAppLayoutWrapper.setVisibility(View.VISIBLE);
      flagsLayoutWrapper.setVisibility(View.GONE);
    } else {
      goodAppLayoutWrapper.setVisibility(View.GONE);
      flagsLayoutWrapper.setVisibility(View.VISIBLE);
      setFlagValues(file);
    }
  }

  private void setFlagValues(GetAppMeta.GetAppMetaFile file) {
    try {
      GetAppMeta.GetAppMetaFile.Flags flags = file.getFlags();

      if (flags != null && flags.getVotes() != null && !flags.getVotes()
          .isEmpty()) {
        for (final GetAppMeta.GetAppMetaFile.Flags.Vote vote : flags.getVotes()) {
          applyCount(vote.getType(), vote.getCount());
        }
      }
    } catch (NullPointerException ex) {
      CrashReport.getInstance()
          .log(ex);
    }
  }

  private void applyCount(GetAppMeta.GetAppMetaFile.Flags.Vote.Type type, int count) {
    String countAsString = Integer.toString(count);
    switch (type) {
      case GOOD:
        workingWellText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case VIRUS:
        virusText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case FAKE:
        fakeAppText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case LICENSE:
        needsLicenceText.setText(NumberFormat.getIntegerInstance()
            .format(Double.parseDouble(countAsString)));
        break;

      case FREEZE:
        break;

      default:
        throw new IllegalArgumentException("Unable to find Type " + type.name());
    }
  }

  private void setDeveloperDetails(DetailedApp app) {
    if (!TextUtils.isEmpty(app.getDeveloper()
        .getWebsite())) {
      String website = app.getDeveloper()
          .getWebsite();
      String websiteCompositeString = String.format(getString(R.string.developer_website), website);
      SpannableString compositeSpan = new SpannableString(websiteCompositeString);
      compositeSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)),
          websiteCompositeString.indexOf(website),
          websiteCompositeString.indexOf(website) + website.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      infoWebsite.setText(compositeSpan);
    } else {
      infoWebsite.setText(
          String.format(getString(R.string.developer_website), getString(R.string.not_available)));
    }

    if (!TextUtils.isEmpty(app.getDeveloper()
        .getEmail())) {
      String email = app.getDeveloper()
          .getEmail();
      String emailCompositeString = String.format(getString(R.string.developer_email), email);
      SpannableString compositeSpan = new SpannableString(emailCompositeString);
      compositeSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)),
          emailCompositeString.indexOf(email), emailCompositeString.indexOf(email) + email.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      infoEmail.setText(compositeSpan);
    } else {
      infoEmail.setText(
          String.format(getString(R.string.developer_email), getString(R.string.not_available)));
    }

    if (!TextUtils.isEmpty(app.getDeveloper()
        .getPrivacy())) {
      String privacy = app.getDeveloper()
          .getPrivacy();
      String privacyCompositeString =
          String.format(getString(R.string.developer_privacy_policy), privacy);
      SpannableString compositeSpan = new SpannableString(privacyCompositeString);
      compositeSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.black)),
          privacyCompositeString.indexOf(privacy),
          privacyCompositeString.indexOf(privacy) + privacy.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      infoPrivacy.setText(compositeSpan);
    } else {
      infoPrivacy.setText(String.format(getString(R.string.developer_privacy_policy),
          getString(R.string.not_available)));
    }
  }

  private void showReviews(boolean hasReviews, DetailedApp app) {
    topReviewsProgress.setVisibility(View.GONE);
    int usersToVote = app.getStats()
        .getGlobalRating()
        .getTotal();
    float ratingAvg = app.getStats()
        .getRating()
        .getAvg();
    reviewUsers.setText(AptoideUtils.StringU.withSuffix(usersToVote));
    avgReviewScore.setText(String.format(Locale.getDefault(), "%.1f", ratingAvg));
    avgReviewScoreBar.setRating(ratingAvg);

    if (hasReviews) {
      ratingLayout.setVisibility(View.VISIBLE);
      emptyReviewsLayout.setVisibility(View.GONE);
      commentsLayout.setVisibility(View.VISIBLE);
      rateAppButtonLarge.setVisibility(View.GONE);
      rateAppButton.setVisibility(View.VISIBLE);
    } else {
      ratingLayout.setVisibility(View.VISIBLE);
      emptyReviewsLayout.setVisibility(View.VISIBLE);
      commentsLayout.setVisibility(View.GONE);
      rateAppButtonLarge.setVisibility(View.VISIBLE);
      rateAppButton.setVisibility(View.INVISIBLE);

      if (usersToVote == 0) {
        emptyReviewTextView.setText(R.string.be_the_first_to_rate_this_app);
      }
    }
  }

  private List<AppViewSimilarApp> mapToSimilar(SimilarAppsViewModel similarApps) {
    List<AppViewSimilarApp> resultList = new ArrayList<>();

    resultList.add(new AppViewSimilarApp(null, similarApps.getAd()));
    for (Application app : similarApps.getRecommendedApps())
      resultList.add(new AppViewSimilarApp(app, null));

    return resultList;
  }

  private void showHideOptionsMenu(boolean visible) {
    for (int i = 0; i < menu.size(); i++) {
      MenuItem item = menu.getItem(i);
      showHideOptionsMenu(item, visible);
    }
  }

  protected void showHideOptionsMenu(@Nullable MenuItem item, boolean visible) {
    if (item != null) {
      item.setVisible(visible);
    }
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    install = null;
    downloadInfoLayout = null;
  }

  public void setupToolbar() {

    toolbar.setTitle("");

    final AppCompatActivity activity = (AppCompatActivity) getActivity();
    activity.setSupportActionBar(toolbar);
    actionBar = activity.getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setTitle(toolbar.getTitle());
    }
  }

  @Override public Observable<DownloadAppViewModel.Action> installAppClick() {
    return RxView.clicks(install)
        .map(__ -> action);
  }

  @Override public Observable<Boolean> showRootInstallWarningPopup() {
    return GenericDialogs.createGenericYesNoCancelMessage(this.getContext(), null,
        getResources().getString(R.string.root_access_dialog))
        .map(response -> (response.equals(YES)));
  }

  @Override public void showDownloadAppModel(DownloadAppViewModel model) {
    this.action = model.getAction();
    if (model.isDownloading()) {
      downloadInfoLayout.setVisibility(View.VISIBLE);
      install.setVisibility(View.GONE);
      setDownloadState(model.getProgress(), model.getDownloadState());
    } else {
      downloadInfoLayout.setVisibility(View.GONE);
      install.setVisibility(View.VISIBLE);
      setButtonText(model.getAction());
    }
  }

  @Override public void openApp(String packageName) {
    AptoideUtils.SystemU.openApp(packageName, getContext().getPackageManager(), getContext());
  }

  @Override public Observable<Boolean> showDowngradeMessage() {
    return GenericDialogs.createGenericContinueCancelMessage(getContext(), null,
        getContext().getResources()
            .getString(R.string.downgrade_warning_dialog))
        .map(eResponse -> eResponse.equals(YES));
  }

  @Override public void showDowngradingMessage() {
    Snackbar.make(getView(), R.string.downgrading_msg, Snackbar.LENGTH_SHORT)
        .show();
  }

  @Override public Observable<Void> pauseDownload() {
    return RxView.clicks(pauseDownload);
  }

  @Override public Observable<Void> resumeDownload() {
    return RxView.clicks(resumeDownload);
  }

  @Override public Observable<Void> cancelDownload() {
    return RxView.clicks(cancelDownload);
  }

  private void setDownloadState(int progress, DownloadAppViewModel.DownloadState downloadState) {
    switch (downloadState) {
      case ACTIVE:
        downloadProgressBar.setIndeterminate(false);
        downloadProgressBar.setProgress(progress);
        pauseDownload.setVisibility(View.VISIBLE);
        cancelDownload.setVisibility(View.GONE);
        resumeDownload.setVisibility(View.GONE);
        break;
      case INDETERMINATE:
        downloadProgressBar.setIndeterminate(true);
        pauseDownload.setVisibility(View.VISIBLE);
        cancelDownload.setVisibility(View.GONE);
        resumeDownload.setVisibility(View.GONE);
        break;
      case PAUSE:
        downloadProgressBar.setIndeterminate(false);
        downloadProgressBar.setProgress(progress);
        pauseDownload.setVisibility(View.GONE);
        cancelDownload.setVisibility(View.VISIBLE);
        resumeDownload.setVisibility(View.VISIBLE);
        break;
      case COMPLETE:
        downloadProgressBar.setIndeterminate(true);
        pauseDownload.setVisibility(View.VISIBLE);
        cancelDownload.setVisibility(View.GONE);
        resumeDownload.setVisibility(View.GONE);
        break;
      case ERROR:
        // TODO: 5/10/18 define error state
        break;
    }
  }

  private void setButtonText(DownloadAppViewModel.Action action) {
    switch (action) {
      case UPDATE:
        install.setText(getResources().getString(R.string.appview_button_update));
        break;
      case INSTALL:
        install.setText(getResources().getString(R.string.appview_button_install));
        break;
      case OPEN:
        install.setText(getResources().getString(R.string.appview_button_open));
        break;
      case DOWNGRADE:
        install.setText(getResources().getString(R.string.appview_button_downgrade));
        break;
    }
  }

  public enum BundleKeys {
    APP_ID, STORE_NAME, STORE_THEME, MINIMAL_AD, PACKAGE_NAME, SHOULD_INSTALL, MD5, UNAME,
  }
}
