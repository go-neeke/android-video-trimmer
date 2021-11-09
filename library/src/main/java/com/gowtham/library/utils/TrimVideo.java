package com.gowtham.library.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.crystal.crystalrangeseekbar.widgets.CrystalRangeSeekbar;
import com.google.gson.Gson;
import com.gowtham.library.R;
import com.gowtham.library.ui.ActVideoTrimmer;

import java.io.File;
import java.util.Calendar;
import java.util.Objects;
import java.util.concurrent.Executors;

public class TrimVideo {

    public static final String TRIM_VIDEO_OPTION = "trim_video_option",
            TRIM_VIDEO_URI = "trim_video_uri", TRIMMED_VIDEO_PATH = "trimmed_video_path";

    public static ActivityBuilder activity(String uri) {
        return new ActivityBuilder(uri);
    }

    public static CompressBuilder compress(Activity activity, String uri, CompressBuilderListener listener) {
        return new CompressBuilder(activity, uri, listener);
    }

    public static String getTrimmedVideoPath(Intent intent) {
        return intent.getStringExtra(TRIMMED_VIDEO_PATH);
    }

    public static final class ActivityBuilder {

        @Nullable
        private final String videoUri;

        private final TrimVideoOptions options;

        public ActivityBuilder(@Nullable String videoUri) {
            this.videoUri = videoUri;
            options = new TrimVideoOptions();
            options.trimType = TrimType.DEFAULT;
        }

        public ActivityBuilder setEnableEdit(final boolean isEnableEdit) {
            options.isEnableEdit = isEnableEdit;
            return this;
        }

        public ActivityBuilder setTrimType(final TrimType trimType) {
            options.trimType = trimType;
            return this;
        }

        public ActivityBuilder setLocal(final String local) {
            options.local = local;
            return this;
        }

        public ActivityBuilder setHideSeekBar(final boolean hide) {
            options.hideSeekBar = hide;
            return this;
        }

        public ActivityBuilder setCompressOption(final CompressOption compressOption) {
            options.compressOption = compressOption;
            return this;
        }

        public ActivityBuilder setFileName(final String fileName) {
            options.fileName = fileName;
            return this;
        }

        public ActivityBuilder showFileLocationAlert() {
            options.showFileLocationAlert = true;
            return this;
        }

        public ActivityBuilder setAccurateCut(final boolean accurate) {
            options.accurateCut = accurate;
            return this;
        }

        public ActivityBuilder setMinDuration(final long minDuration) {
            options.minDuration = minDuration;
            return this;
        }

        public ActivityBuilder setFixedDuration(final long fixedDuration) {
            options.fixedDuration = fixedDuration;
            return this;
        }

        public ActivityBuilder setMinToMax(long min, long max) {
            options.minToMax = new long[]{min, max};
            return this;
        }

        public ActivityBuilder setTitle(String title) {
            options.title = title;
            return this;
        }

        public ActivityBuilder setExecute(boolean isExecute) {
            options.isExecute = isExecute;
            return this;
        }

        public void start(Activity activity,
                          ActivityResultLauncher<Intent> launcher) {
            validate();
            launcher.launch(getIntent(activity));
        }

        public void start(Fragment fragment, ActivityResultLauncher<Intent> launcher) {
            validate();
            launcher.launch(getIntent(fragment.getActivity()));
        }

        private void validate() {
            if (videoUri == null)
                throw new NullPointerException("VideoUri cannot be null.");
            if (videoUri.isEmpty())
                throw new IllegalArgumentException("VideoUri cannot be empty");
            if (options.trimType == null)
                throw new NullPointerException("TrimType cannot be null");
            if (options.minDuration < 0)
                throw new IllegalArgumentException("Cannot set min duration to a number < 1");
            if (options.fixedDuration < 0)
                throw new IllegalArgumentException("Cannot set fixed duration to a number < 1");
            if (options.trimType == TrimType.MIN_MAX_DURATION && options.minToMax == null)
                throw new IllegalArgumentException("Used trim type is TrimType.MIN_MAX_DURATION." +
                        "Give the min and max duration");
            if (options.minToMax != null) {
                if ((options.minToMax[0] < 0 || options.minToMax[1] < 0))
                    throw new IllegalArgumentException("Cannot set min to max duration to a number < 1");
                if ((options.minToMax[0] > options.minToMax[1]))
                    throw new IllegalArgumentException("Minimum duration cannot be larger than max duration");
                if ((options.minToMax[0] == options.minToMax[1]))
                    throw new IllegalArgumentException("Minimum duration cannot be same as max duration.Use Fixed duration");
            }
        }

        private Intent getIntent(Activity activity) {
            Intent intent = new Intent(activity, ActVideoTrimmer.class);
//            Gson gson = new Gson();
            Bundle bundle = new Bundle();
            bundle.putString(TRIM_VIDEO_URI, videoUri);
//            bundle.putString(TRIM_VIDEO_OPTION, gson.toJson(options));
            intent.putExtras(bundle);
            return intent;
        }
    }

    public static final class CompressBuilder {
        @Nullable
        private final Activity activity;

        private long lastMinValue = 0;

        private long lastMaxValue = 0;

        private final TrimVideoOptions options;

        private String outputPath;

        private final CompressBuilderListener listener;

        @Nullable
        private Uri videoUri;

        public CompressBuilder(@Nullable Activity activity, @Nullable String videoUri, CompressBuilderListener listener) {
            this.activity = activity;
            this.options = new TrimVideoOptions();
            this.listener = listener;
            try {
                Runnable fileUriRunnable = () -> {
                    this.videoUri = Uri.parse(videoUri);
                    String path = FileUtils.getPath(activity, this.videoUri);
                    this.videoUri = Uri.parse(path);

                    this.outputPath = getFileName();
                    this.lastMinValue = 0;
                    this.lastMaxValue = TrimmerUtils.getDuration(activity, this.videoUri);

                    Log.d("A.lee", "this.lastMaxValue" + this.lastMaxValue);
                };
                Executors.newSingleThreadExecutor().execute(fileUriRunnable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public CompressBuilder setCompressOption(final CompressOption compressOption) {
            options.compressOption = compressOption;
            return this;
        }

        public void trimVideo() {
            //not exceed given maxDuration if has given

            LogMessage.v("outputPath::" + outputPath + new File(outputPath).exists());
            LogMessage.v("sourcePath::" + this.videoUri);

            listener.onProcessing();

            String[] complexCommand;
            if (options.compressOption != null) {
                complexCommand = getCompressionCmd();
            } else {
                Log.d("A.lee", "trimVideo");
                //no changes in video quality
                //fastest trimming command however, result duration
                //will be low accurate(2-3 secs)
                complexCommand = new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                        "-i", String.valueOf(this.videoUri),
                        "-t",
                        TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue),
                        "-async", "1", "-strict", "-2", "-c", "copy", outputPath};
            }
            execFFmpegBinary(complexCommand);
        }

        private void execFFmpegBinary(final String[] command) {
            try {
                new Thread(() -> {
                    int result = FFmpeg.execute(command);
                    if (result == 0) {
                        this.listener.onSuccess(outputPath);

                    } else if (result == 255) {
                        LogMessage.v("Command cancelled");
                        this.listener.onFailed();
                    } else {
                        activity.runOnUiThread(() ->
                                {
                                    Toast.makeText(this.activity, "Failed to trim", Toast.LENGTH_SHORT).show();
                                    this.listener.onFailed();
                                }
                        );
                    }
                }).start();


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String getFileName() {
            String path = activity.getExternalFilesDir("TrimmedVideo").getPath();
            Calendar calender = Calendar.getInstance();
            String fileDateTime = calender.get(Calendar.YEAR) + "_" +
                    calender.get(Calendar.MONTH) + "_" +
                    calender.get(Calendar.DAY_OF_MONTH) + "_" +
                    calender.get(Calendar.HOUR_OF_DAY) + "_" +
                    calender.get(Calendar.MINUTE) + "_" +
                    calender.get(Calendar.SECOND);
            String fName = "trimmed_video_";
            File newFile = new File(path + File.separator +
                    (fName) + fileDateTime + "."
                    + TrimmerUtils.getFileExtension(activity, this.videoUri));
            return String.valueOf(newFile);
        }

        private String[] getCompressionCmd() {
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(String.valueOf(this.videoUri));
            String height = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            String width = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            int w = TrimmerUtils.clearNull(width).isEmpty() ? 0 : Integer.parseInt(width);
            int h = Integer.parseInt(height);
            int rotation = TrimmerUtils.getVideoRotation(activity, this.videoUri);
            if (rotation == 90 || rotation == 270) {
                int temp = w;
                w = h;
                h = temp;
            }
            //Default compression option
            if (options.compressOption.getWidth() != 0 || options.compressOption.getHeight() != 0
                    || !options.compressOption.getBitRate().equals("0k")) {
                return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                        "-i", String.valueOf(this.videoUri), "-s", options.compressOption.getWidth() + "x" +
                        options.compressOption.getHeight(),
                        "-r", String.valueOf(options.compressOption.getFrameRate()),
                        "-vcodec", "mpeg4", "-b:v",
                        options.compressOption.getBitRate(), "-b:a", "48000", "-ac", "2", "-ar",
                        "22050", "-t",
                        TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
            }
            //Dividing high resolution video by 2(ex: taken with camera)
            else if (w >= 800) {
                w = w / 2;
                h = Integer.parseInt(height) / 2;
                return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                        "-i", String.valueOf(this.videoUri),
                        "-s", w + "x" + h, "-r", "30",
                        "-vcodec", "mpeg4", "-b:v",
                        "1M", "-b:a", "48000", "-ac", "2", "-ar", "22050",
                        "-t",
                        TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
            } else {
                return new String[]{"-ss", TrimmerUtils.formatCSeconds(lastMinValue),
                        "-i", String.valueOf(this.videoUri), "-s", w + "x" + h, "-r",
                        "30", "-vcodec", "mpeg4", "-b:v",
                        "400K", "-b:a", "48000", "-ac", "2", "-ar", "22050",
                        "-t",
                        TrimmerUtils.formatCSeconds(lastMaxValue - lastMinValue), outputPath};
            }
        }

    }

    public interface CompressBuilderListener {
        void onProcessing();

        void onSuccess(String outputPath);

        void onFailed();
    }
}
