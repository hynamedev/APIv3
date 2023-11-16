package net.frozenorb.apiv3.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.frozenorb.apiv3.domain.Rank;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PermissionUtils {

	public static Map<String, Boolean> mergePermissions(Map<String, Boolean> current, Map<String, Boolean> merge) {
		Map<String, Boolean> result = new HashMap<>(current);

		result.putAll(merge);

		return result;
	}

	public static Map<String, Boolean> mergeUpTo(Map<String, List<String>> raw, Rank upTo) {
		Map<String, Boolean> result = new HashMap<>();
		List<Rank> mergeQueue = new LinkedList<>();
		Rank merge = upTo;

		while (merge != null) {
			mergeQueue.add(0, merge);
			merge = Rank.findById(merge.getInheritsFromId());
		}

		for (Rank rank : mergeQueue) {
			Map<String, Boolean> rankPermissions = convertFromList(raw.get(rank.getId()));

			// If there's no permissions defined for this rank just skip it.
			if (!rankPermissions.isEmpty()) {
				result = mergePermissions(result, rankPermissions);
			}
		}

		return result;
	}

	private static Map<String, Boolean> convertFromList(List<String> permissionsList) {
		if (permissionsList == null) {
			return ImmutableMap.of();
		}

		Map<String, Boolean> permissionsMap = new HashMap<>();

		permissionsList.forEach((permission) -> {
			if (permission.startsWith("-")) {
				permissionsMap.put(permission.substring(1), false);
			} else {
				permissionsMap.put(permission, true);
			}
		});

		return permissionsMap;
	}

	public static List<String> convertToList(Map<String, Boolean> permissionsMap) {
		if (permissionsMap == null) {
			return ImmutableList.of();
		}

		List<String> permissionsList = new LinkedList<>();

		permissionsMap.forEach((permission, granted) -> {
			permissionsList.add((granted ? "" : "-") + permission);
		});

		return permissionsList;
	}

}