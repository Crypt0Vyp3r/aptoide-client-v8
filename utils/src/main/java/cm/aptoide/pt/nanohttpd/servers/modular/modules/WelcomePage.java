package cm.aptoide.pt.nanohttpd.servers.modular.modules;

import cm.aptoide.pt.nanohttpd.servers.modular.asset.html.HtmlLocalizedAssetServer;
import java.util.HashMap;

/**
 * Created by neuro on 08-05-2017.
 */

public class WelcomePage extends HtmlLocalizedAssetServer {

  public WelcomePage(HashMap<String, String> tokensMap) {
    super("/", "share_apk_welcome.html", "{{", "}}", tokensMap);
  }
}
