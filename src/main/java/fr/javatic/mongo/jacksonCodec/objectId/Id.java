package fr.javatic.mongo.jacksonCodec.objectId;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonProperty("_id")
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonDeserialize
public @interface Id {}