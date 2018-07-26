package cm.aptoide.pt.home;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import cm.aptoide.pt.R;
import cm.aptoide.pt.view.app.AppViewHolder;
import cm.aptoide.pt.view.app.Application;
import java.text.DecimalFormat;
import java.util.List;
import rx.subjects.PublishSubject;

/**
 * Created by jdandrade on 07/03/2018.
 */

class AppsInBundleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static final int APP = R.layout.app_home_item;
  private final DecimalFormat oneDecimalFormatter;
  private final PublishSubject<HomeEvent> appClickedEvents;
  private HomeBundle homeBundle;
  private int bundlePosition;
  private List<Application> apps;

  AppsInBundleAdapter(List<Application> apps, DecimalFormat oneDecimalFormatter,
      PublishSubject<HomeEvent> appClickedEvents) {
    this.apps = apps;
    this.oneDecimalFormatter = oneDecimalFormatter;
    this.appClickedEvents = appClickedEvents;
    this.homeBundle = null;
    this.bundlePosition = -1;
  }

  @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new AppInBundleViewHolder(LayoutInflater.from(parent.getContext())
        .inflate(APP, parent, false), appClickedEvents, oneDecimalFormatter);
  }

  @Override public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
    ((AppViewHolder) viewHolder).setApp(apps.get(position), homeBundle, bundlePosition, position);
  }

  @Override public int getItemCount() {
    return apps.size();
  }

  public void update(List<Application> apps) {
    this.apps = apps;
    notifyDataSetChanged();
  }

  public void updateBundle(HomeBundle homeBundle, int position) {
    this.homeBundle = homeBundle;
    this.bundlePosition = position;
  }
}
