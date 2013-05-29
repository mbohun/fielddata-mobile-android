package au.org.ala.fielddata.mobile;

import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;

import android.app.ActivityManager;
import android.app.Application;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiscCache;
import com.nostra13.universalimageloader.core.DefaultConfigurationFactory;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * Performs application initialisation.
 */
public class FieldDataApp extends Application {

	private ThreadPoolExecutor executor;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		initialiseImageLoader();
	}
	
	public void initialiseImageLoader() {
		
		executor = (ThreadPoolExecutor)DefaultConfigurationFactory.createExecutor(2, 3, ImageLoaderConfiguration.Builder.DEFAULT_TASK_PROCESSING_TYPE);
		File cacheDir = new File(getCacheDir(), "images"); 
		int memoryInMB = ((ActivityManager)getSystemService(ACTIVITY_SERVICE)).getMemoryClass();
		long totalAppHeap = memoryInMB * 1024 * 1024;
		int cacheSize =  (int)totalAppHeap/4; // Use a max of a quarter of the heap for image cache.
		DisplayImageOptions options = new DisplayImageOptions.Builder().
				cacheInMemory().
				cacheOnDisc().
				build();
		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
			.memoryCacheSize(cacheSize)
			.taskExecutor(executor)
			.defaultDisplayImageOptions(options)
			.discCache(new UnlimitedDiscCache(cacheDir))
			.build();
		ImageLoader.getInstance().init(config);
	}
	
	public ThreadPoolExecutor getImageLoaderExecutor() {
		return executor;
	}
}
