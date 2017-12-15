package cm.aptoide.pt;

import cm.aptoide.pt.account.view.LoginSignUpCredentialsFragment;
import cm.aptoide.pt.account.view.MyAccountFragment;
import cm.aptoide.pt.account.view.store.ManageStoreFragment;
import cm.aptoide.pt.account.view.user.ManageUserFragment;
import cm.aptoide.pt.addressbook.view.AddressBookFragment;
import cm.aptoide.pt.updates.view.rollback.RollbackFragment;
import cm.aptoide.pt.view.FragmentScope;
import dagger.Subcomponent;

@FragmentScope @Subcomponent(modules = { FragmentModule.class })
public interface FragmentComponent {

  void inject(AddressBookFragment addressBookFragment);

  void inject(RollbackFragment rollbackFragment);

  void inject(LoginSignUpCredentialsFragment loginSignUpCredentialsFragment);

  void inject(ManageUserFragment manageUserFragment);

  void inject(ManageStoreFragment manageStoreFragment);

  void inject(MyAccountFragment myAccountFragment);
}
