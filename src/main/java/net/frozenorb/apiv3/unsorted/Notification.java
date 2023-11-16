package net.frozenorb.apiv3.unsorted;

import net.frozenorb.apiv3.domain.NotificationTemplate;

import java.util.Map;

import lombok.Getter;

public final class Notification {

	@Getter private final String subject;
	@Getter private final String body;

	public Notification(NotificationTemplate template, Map<String, Object> subjectReplacements, Map<String, Object> bodyReplacements) {
		this.subject = template.fillSubject(subjectReplacements);
		this.body = template.fillBody(bodyReplacements);
	}

}