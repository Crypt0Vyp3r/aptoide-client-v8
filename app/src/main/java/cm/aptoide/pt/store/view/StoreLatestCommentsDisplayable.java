package cm.aptoide.pt.store.view;

import cm.aptoide.pt.R;
import cm.aptoide.pt.comment.CommentMapper;
import cm.aptoide.pt.comment.CommentsListManager;
import cm.aptoide.pt.comment.CommentsNavigator;
import cm.aptoide.pt.dataprovider.model.v7.Comment;
import cm.aptoide.pt.view.recycler.displayable.Displayable;
import java.util.Collections;
import java.util.List;

public class StoreLatestCommentsDisplayable extends Displayable {

  private final long storeId;
  private final List<Comment> comments;
  private CommentMapper commentMapper;
  private CommentsNavigator commentsNavigator;
  private CommentsListManager commentsListManager;
  private String storeName;

  public StoreLatestCommentsDisplayable() {
    this.storeId = -1;
    this.comments = Collections.emptyList();
  }

  public StoreLatestCommentsDisplayable(long storeId, String storeName, List<Comment> comments,
      CommentMapper commentMapper, CommentsNavigator commentsNavigator,
      CommentsListManager commentsListManager) {
    this.storeId = storeId;
    this.storeName = storeName;
    this.comments = comments;
    this.commentMapper = commentMapper;
    this.commentsNavigator = commentsNavigator;
    this.commentsListManager = commentsListManager;
  }

  public List<Comment> getComments() {
    return comments;
  }

  public long getStoreId() {
    return storeId;
  }

  @Override protected Configs getConfig() {
    return new Configs(1, true);
  }

  @Override public int getViewLayout() {
    return R.layout.displayable_grid_latest_store_comments;
  }

  public String getStoreName() {
    return storeName;
  }

  public CommentMapper getCommentMapper() {
    return commentMapper;
  }

  public CommentsNavigator getCommentsNavigator() {
    return commentsNavigator;
  }

  public CommentsListManager getCommentsListManager() {
    return commentsListManager;
  }
}
