/*
 * Copyright (c) 2016.
 * Modified by Neurophobic Animal on 12/05/2016.
 */

package cm.aptoide.pt.model.v7.store;

import cm.aptoide.pt.model.v7.Event;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Created by neuro on 27-04-2016.
 */
@Data @Accessors(chain = true) public class Store {
  private long id;
  private String name;
  private String avatar;
  @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "UTC") private Date added;
  @JsonFormat(pattern = "yyyy-MM-dd hh:mm:ss", timezone = "UTC") private Date modified;
  private Appearance appearance;
  private Stats stats;
  @JsonProperty("list") private List<SocialChannel> socialChannels;

  @Data public static class Stats {
    private int apps;
    private int subscribers;
    private int downloads;
  }

  @Data @NoArgsConstructor @AllArgsConstructor() public static class Appearance {
    private String theme;
    private String description;
  }

  @Data public static class SocialChannel {
    private String label;
    private String tag;
    private String graphic;
    private String map;
    private Event event;
  }
}
