package cm.aptoide.pt.v8engine.billing.view.boacompra;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;
import cm.aptoide.accountmanager.AptoideAccountManager;
import cm.aptoide.pt.v8engine.V8Engine;
import cm.aptoide.pt.v8engine.billing.Billing;
import cm.aptoide.pt.v8engine.billing.BillingAnalytics;
import cm.aptoide.pt.v8engine.billing.view.BillingNavigator;
import cm.aptoide.pt.v8engine.billing.view.PaymentActivity;
import cm.aptoide.pt.v8engine.billing.view.PaymentThrowableCodeMapper;
import cm.aptoide.pt.v8engine.billing.view.PurchaseBundleMapper;
import cm.aptoide.pt.v8engine.billing.view.WebViewFragment;

public class BoaCompraFragment extends WebViewFragment {

  private Billing billing;
  private BillingAnalytics billingAnalytics;
  private AptoideAccountManager accountManager;

  public static Fragment create(Bundle bundle) {
    final BoaCompraFragment fragment = new BoaCompraFragment();
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    billing = ((V8Engine) getContext().getApplicationContext()).getBilling();
    billingAnalytics = ((V8Engine) getContext().getApplicationContext()).getBillingAnalytics();
    accountManager = ((V8Engine) getContext().getApplicationContext()).getAccountManager();
  }

  @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    attachPresenter(new BoaCompraPresenter(this, billing, billingAnalytics,
        new BillingNavigator(new PurchaseBundleMapper(new PaymentThrowableCodeMapper()),
            getActivityNavigator(), getFragmentNavigator(), accountManager),
        getArguments().getString(PaymentActivity.EXTRA_APPLICATION_ID),
        getArguments().getString(PaymentActivity.EXTRA_PRODUCT_ID),
        getArguments().getString(PaymentActivity.EXTRA_DEVELOPER_PAYLOAD),
        getArguments().getString(PaymentActivity.EXTRA_PAYMENT_METHOD_NAME)), savedInstanceState);
  }
}