package cm.aptoide.pt.blacklist;

public class BlacklistUnitMapper {

  public BlacklistManager.BlacklistUnit mapToBlacklistUnit(String blacklistKey) {

    if (blacklistKey.equals("WALLET_ADS_OFFER_51")) {
      return BlacklistManager.BlacklistUnit.WALLET_ADS_OFFER;
    } else if (blacklistKey.equals("appc_card_info_1")) {
      return BlacklistManager.BlacklistUnit.APPC_CARD_INFO;
    } else {
      throw new IllegalArgumentException(
          "Wrong blacklist key. Please, make sure you are passing the correct type and id.");
    }
  }
}
