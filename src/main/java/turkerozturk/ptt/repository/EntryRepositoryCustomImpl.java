package turkerozturk.ptt.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import turkerozturk.ptt.entity.Entry;

import java.util.ArrayList;
import java.util.List;

@Repository
public class EntryRepositoryCustomImpl implements EntryRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<Entry> findEntriesByFilter(List<Long> topicIds, long startEpoch, long endEpoch, List<Integer> statuses) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Entry> cq = cb.createQuery(Entry.class);
        Root<Entry> root = cq.from(Entry.class);

        List<Predicate> predicates = new ArrayList<>();

        // dateMillisYmd >= startEpoch
        predicates.add(cb.greaterThanOrEqualTo(root.get("dateMillisYmd"), startEpoch));
        // dateMillisYmd <= endEpoch
        predicates.add(cb.lessThanOrEqualTo(root.get("dateMillisYmd"), endEpoch));

        // Topic filtresi (boş değilse)
        if (topicIds != null && !topicIds.isEmpty()) {
            predicates.add(root.get("topic").get("id").in(topicIds));
        }

        // Status filtresi (boş değilse)
        if (statuses != null && !statuses.isEmpty()) {
            predicates.add(root.get("status").in(statuses));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        return em.createQuery(cq).getResultList();
    }
}
