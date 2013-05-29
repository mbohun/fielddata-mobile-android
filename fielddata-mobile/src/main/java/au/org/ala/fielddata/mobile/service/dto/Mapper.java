package au.org.ala.fielddata.mobile.service.dto;

import android.content.Context;
import android.location.Location;
import au.org.ala.fielddata.mobile.model.Record.StringValue;
import au.org.ala.fielddata.mobile.service.StringValueAdapter;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Mapper {

	public static Gson getGson(Context ctx) {
		return getGson(ctx, false);
	}
	
	public static Gson getGson(Context ctx, boolean forUpload) {
		GsonBuilder builder = new GsonBuilder();
		builder.setFieldNamingPolicy(FieldNamingPolicy.IDENTITY);
		if (forUpload) {
			builder.registerTypeHierarchyAdapter(StringValue.class, new StringValueAdapter(ctx));
		}
		builder.registerTypeAdapter(Location.class, new LocationSerializer());
		return builder.create();
	}
}
