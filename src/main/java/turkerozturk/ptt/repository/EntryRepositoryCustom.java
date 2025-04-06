package turkerozturk.ptt.repository;

import turkerozturk.ptt.entity.Entry;

import java.util.List;

public interface EntryRepositoryCustom {
    List<Entry> findEntriesByFilter(List<Long> topicIds, long startEpoch, long endEpoch, List<Integer> statuses);
}
