package cm.aptoide.pt;

import cm.aptoide.pt.abtesting.experiments.IronSourceInterstitialAdExperiment;
import cm.aptoide.pt.view.FragmentScope;
import cm.aptoide.pt.view.wizard.WizardFragmentProvider;
import dagger.Module;
import dagger.Provides;
import javax.inject.Named;

@Module public class FlavourFragmentModule {

  public FlavourFragmentModule() {
  }

  @FragmentScope @Provides
  IronSourceInterstitialAdExperiment providesIronSourceInterstitialAdExperiment() {
    return new IronSourceInterstitialAdExperiment();
  }

  @FragmentScope @Provides WizardFragmentProvider providesWizardFragmentProvider(@Named("aptoide-theme") String theme) {
    return new WizardFragmentProvider(theme);
  }

}
