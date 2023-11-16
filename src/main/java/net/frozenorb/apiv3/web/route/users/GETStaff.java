package net.frozenorb.apiv3.web.route.users;

import net.frozenorb.apiv3.APIv3;
import net.frozenorb.apiv3.domain.Grant;
import net.frozenorb.apiv3.domain.Rank;
import net.frozenorb.apiv3.domain.User;
import net.frozenorb.apiv3.util.SyncUtils;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Component
public final class GETStaff implements Handler<RoutingContext> {

    public void handle(RoutingContext ctx) {
        Map<String, Rank> staffRanks = new HashMap<>();

        Rank.findAll().forEach(rank -> {
            if (rank.isStaffRank()) {
                staffRanks.put(rank.getId(), rank);
            }
        });

        Map<String, List<User>> result = new TreeMap<>((a, b) -> {
            Rank aRank = staffRanks.get(a);
            Rank bRank = staffRanks.get(b);

            return Integer.compare(bRank.getGeneralWeight(), aRank.getGeneralWeight());
        });

        List<Grant> staffGrants = SyncUtils.runBlocking(v -> Grant.findByRank(staffRanks.values(), v));
        staffGrants.sort((a, b) -> a.getAddedAt().compareTo(b.getAddedAt()));

        for (Grant staffGrant : staffGrants) {
            if (staffGrant.isActive()) {
                User user = SyncUtils.runBlocking(v -> User.findById(staffGrant.getUser(), v));
                Rank rank = staffRanks.get(staffGrant.getRank());

                if (!result.containsKey(rank.getId())) {
                    result.put(rank.getId(), new ArrayList<>());
                }

                result.get(rank.getId()).add(user);
            }
        }

        APIv3.respondJson(ctx, 200, result);
    }

}