package beproject.cbir;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import beproject.cbir.database.TagsDatabase;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;

public class KeywordSearchActivity extends Activity {

	ProgressBar pb;
	ImageLoader imageLoader = ImageLoader.getInstance();
	MultiAutoCompleteTextView textView;

	String[] imageUrls;

	DisplayImageOptions options;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		super.setContentView(R.layout.keywordsearch);

		traverse(dir);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_dropdown_item_1line, Tags);
		textView = (MultiAutoCompleteTextView) findViewById(R.id.original_text);
		textView.setAdapter(adapter);
		textView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

		textView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					doWork();
					return true;
				}
				return false;
			}
		});

		ImageButton ib = (ImageButton) findViewById(R.id.ibSearch);
		pb = (ProgressBar) findViewById(R.id.pbsuggest);
		ib.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				doWork();
			}
		});

	}

	void doWork() {

		listView = (GridView) findViewById(R.id.result_list);
		listView.setVisibility(View.VISIBLE);

		Tags = new ArrayList<String>();

		result = new ArrayList<String>();
		mainResult = new ArrayList<String>();

		pb.setVisibility(View.VISIBLE);

		String search = textView.getText().toString();
		if (search.contains(",")) {
			Tagstl = search.split(", ");
		} else {
			Tagstl = new String[1];
			Tagstl[0] = search;

		}

		//
		// Log.d("tagit", "" + Tagstl[i]);

		for (int i = 0; i < Tagstl.length; i++) {
			try {
				TagsDatabase tb = new TagsDatabase(getBaseContext());
				tb.open();
				result.addAll(tb.CheckIfTagExists(Tagstl[i]));
				tb.close();
			} catch (Exception ex) {
				Toast.makeText(getBaseContext(), "No results!!",
						Toast.LENGTH_LONG).show();
				pb.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
				return;
			}

			// Toast.makeText(getBaseContext(), result.toString(),
			// Toast.LENGTH_LONG).show();
		}

		for (int i = 0; i < result.size(); i++)
			if (!mainResult.contains(result.get(i))) {
				if (Collections.frequency(result, result.get(i)) == Tagstl.length) {
					mainResult.add(result.get(i));
				}
			}

		if (mainResult.size() == 0) {
			Toast.makeText(getBaseContext(), "No results!!", Toast.LENGTH_LONG)
					.show();
			pb.setVisibility(View.GONE);
			listView.setVisibility(View.GONE);
			return;
		}

		Log.d("tagit", mainResult.toString());
		imageUrls = new String[mainResult.size()];
		for (int i = 0; i < mainResult.size(); i++) {
			imageUrls[i] = "file://" + mainResult.get(i);
		}

		options = new DisplayImageOptions.Builder()
				.showStubImage(R.drawable.ic_launcher)
				.showImageForEmptyUri(R.drawable.ic_launcher)
				.showImageOnFail(R.drawable.hash).cacheInMemory().cacheOnDisc()
				.bitmapConfig(Bitmap.Config.RGB_565).build();

		((GridView) listView).setAdapter(new ImageAdapter());
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				startImagePagerActivity(position);
			}
		});

		pb.setVisibility(View.GONE);

	}

	private void startImagePagerActivity(int position) {
		Intent intent = new Intent(this, ImagePagerActivity.class);
		intent.putExtra(Extra.IMAGES, imageUrls);
		intent.putExtra(Extra.IMAGE_POSITION, position);
		startActivity(intent);
	}

	public class ImageAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return imageUrls.length;
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ImageView imageView;
			if (convertView == null) {
				imageView = (ImageView) getLayoutInflater().inflate(
						R.layout.item_grid_image, parent, false);
			} else {
				imageView = (ImageView) convertView;
			}

			imageLoader.displayImage(imageUrls[position], imageView, options);

			return imageView;
		}
	}

	List<String> Tags = new ArrayList<String>();
	String[] Tagstl;
	final File dir = new File(Environment.getExternalStorageDirectory()
			+ "/TagIt/.Tags/");
	ArrayList<String> result = new ArrayList<String>();
	ArrayList<String> mainResult = new ArrayList<String>();

	public void traverse(File dir) {
		if (dir.exists()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; ++i) {
				File file = files[i];
				if (file.isDirectory()) {
					Tags.add(file.getName());
				}
			}
		}
	}

	protected static final String STATE_PAUSE_ON_SCROLL = "STATE_PAUSE_ON_SCROLL";
	protected static final String STATE_PAUSE_ON_FLING = "STATE_PAUSE_ON_FLING";

	protected AbsListView listView;

	protected boolean pauseOnScroll = false;
	protected boolean pauseOnFling = true;

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		pauseOnScroll = savedInstanceState.getBoolean(STATE_PAUSE_ON_SCROLL,
				false);
		pauseOnFling = savedInstanceState
				.getBoolean(STATE_PAUSE_ON_FLING, true);
	}

	@Override
	public void onResume() {
		super.onResume();
		applyScrollListener();
	}

	private void applyScrollListener() {
		try {
			listView.setOnScrollListener(new PauseOnScrollListener(imageLoader,
					pauseOnScroll, pauseOnFling));
		} catch (Exception ex) {
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(STATE_PAUSE_ON_SCROLL, pauseOnScroll);
		outState.putBoolean(STATE_PAUSE_ON_FLING, pauseOnFling);
	}

}
