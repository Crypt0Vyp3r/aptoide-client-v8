/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 07/06/2016.
 */

package cm.aptoide.pt.search.suggestionsprovider;

import android.content.res.Resources;
import android.database.MatrixCursor;
import cm.aptoide.pt.R;
import cm.aptoide.pt.crashreports.CrashReport;
import cm.aptoide.pt.search.suggestionsprovider.websocket.SearchWebSocket;
import cm.aptoide.pt.search.suggestionsprovider.websocket.SearchWebSocketProvider;
import cm.aptoide.pt.search.suggestionsprovider.websocket.WebSocketSearchRecentSuggestionsProvider;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class AppSuggestions extends WebSocketSearchRecentSuggestionsProvider {

  private final SearchWebSocketProvider searchWebSocketProvider;
  private final BlockingQueue<MatrixCursor> blockingQueue = new ArrayBlockingQueue<>(1);
  private SearchWebSocket searchSocket;

  public AppSuggestions() {
    super(CrashReport.getInstance());
    searchWebSocketProvider = new SearchWebSocketProvider();
  }

  @Override public BlockingQueue<MatrixCursor> getBlockingQueue() {
    return blockingQueue;
  }

  @Override public String getSearchProvider(Resources resources) {
    return resources.getString(R.string.search_suggestion_provider_authority);
  }

  @Override public SearchWebSocket getSearchSocket() {
    if (searchSocket == null) {
      searchSocket = searchWebSocketProvider.getSearchAppsSocket(blockingQueue);
    }
    return searchSocket;
  }
}
