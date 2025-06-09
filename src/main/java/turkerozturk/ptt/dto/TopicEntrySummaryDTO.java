package turkerozturk.ptt.dto;
public interface TopicEntrySummaryDTO {

 String getCategoryGroupName();
 String getCategoryName();
 String getTopicName();
 Long getTopicId();
 Long getCategoryId();
 Integer getCategoryGroupNumber();
 Long getItemCount();

 Long getPredictionDateMillisYmd();
 Long getLastPastEntryDateMillisYmd();
 Long getFirstWarningEntryDateMillisYmd();
 Long getFirstFutureNeutralEntryDateMillisYmd();
 Long getSomeTimeLater();
 Integer getWeight();
 Integer getPinned(); // because SQLite has no boolean.
 Integer getArchived(); // because SQLite has no boolean.
 Integer getStatus();

 Double getRatio();
}
