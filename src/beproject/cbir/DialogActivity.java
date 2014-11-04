package beproject.cbir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class DialogActivity extends Activity {

	File files = new File(Environment.getExternalStorageDirectory() + "/TagIt/"
			+ ".temp/TS_temp" + ".jpg");

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ScrollView sl = new ScrollView(DialogActivity.this);
		LinearLayout ll = new LinearLayout(DialogActivity.this);
		ll.setOrientation(LinearLayout.VERTICAL);
		final ImageView ivImg = new ImageView(DialogActivity.this);
		final EditText et = new EditText(DialogActivity.this);
		final Button b = new Button(DialogActivity.this);
		ll.addView(ivImg);
		ll.addView(et);
		ll.addView(b);
		sl.addView(ll);
		LayoutParams params = ivImg.getLayoutParams();

		params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.6);

		ivImg.setLayoutParams(params);

		setContentView(sl);

		b.setText("Add");

		Bitmap bitmap = BitmapFactory.decodeFile(files.getPath());
		ivImg.setImageBitmap(bitmap);
		if (bitmap.getHeight() < (int) (getResources().getDisplayMetrics().heightPixels * 0.6))
			ivImg.setScaleType(ScaleType.CENTER);

		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (et.getText().toString().length() == 0) {
					Toast.makeText(getBaseContext(), "Please Enter Tag.",
							Toast.LENGTH_LONG).show();
				} else {
					Intent intent = new Intent();
					intent.putExtra("tag",
							et.getText().toString().toLowerCase(Locale.ENGLISH));
					try {
						copyFile(
								files,
								new File(Environment
										.getExternalStorageDirectory()
										+ "/TagIt/.Tags/"
										+ et.getText().toString()
												.toLowerCase(Locale.ENGLISH)));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					intent.putExtra("path", path);
					setResult(RESULT_OK, intent);
					finish();
				}
			}
		});

	}

	String path;

	public void copyFile(File sourceFile, File targetLocation)
			throws IOException {

		if (!targetLocation.exists()) {
			targetLocation.mkdirs();
		}
		// File[] files = sourceLocation.listFiles();
		// for (File file : files) {
		InputStream in = new FileInputStream(sourceFile);
		OutputStream out = new FileOutputStream(targetLocation + "/"
				+ System.currentTimeMillis() + ".jpg");
		path = targetLocation + "/TS_" + System.currentTimeMillis() + ".jpg";

		// Copy the bits from input stream to output stream
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
		// }

	}

}
