package au.org.ala.fielddata.mobile.service.dto;

import java.lang.reflect.Type;

import android.location.Location;

import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * The LocationSerializer class is responsible for converting a Location
 * object to and from json as relying on defaults results in errors. 
 */
public class LocationSerializer implements JsonSerializer<Location>, JsonDeserializer<Location>,
		InstanceCreator<Location> {
	private static final String ACCURACY = "accuracy";
	private static final String HAS_ACCURACY = "hasAccuracy";
	private static final String LONGITUDE = "longitude";
	private static final String TIME = "time";
	private static final String PROVIDER = "provider";
	private static final String LATITUDE = "latitude";

	public JsonElement serialize(Location location, Type typeOfSrc, JsonSerializationContext context) {
		JsonObject jsonLocation = new JsonObject();
		jsonLocation.add(LATITUDE, new JsonPrimitive(location.getLatitude()));
		jsonLocation.add(LONGITUDE, new JsonPrimitive(location.getLongitude()));
		jsonLocation.add(HAS_ACCURACY, new JsonPrimitive(location.hasAccuracy()));
		jsonLocation.add(ACCURACY, new JsonPrimitive(location.getAccuracy()));
		jsonLocation.add(PROVIDER, new JsonPrimitive(location.getProvider()));
		jsonLocation.add(TIME, new JsonPrimitive(location.getTime()));
		return jsonLocation;
	}

	public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		JsonObject jsonLocation = (JsonObject) json;

		Location location = new Location(jsonLocation.getAsJsonPrimitive(PROVIDER).getAsString());
		location.setLatitude(jsonLocation.getAsJsonPrimitive(LATITUDE).getAsDouble());
		location.setLongitude(jsonLocation.getAsJsonPrimitive(LONGITUDE).getAsDouble());
		location.setTime(jsonLocation.getAsJsonPrimitive(TIME).getAsLong());
		boolean hasAccuracy = jsonLocation.getAsJsonPrimitive(HAS_ACCURACY).getAsBoolean();
		if (hasAccuracy) {
			location.setAccuracy(jsonLocation.getAsJsonPrimitive(ACCURACY).getAsFloat());
		}
		return location;
	}

	public Location createInstance(Type type) {
		return new Location("");
	}

}
