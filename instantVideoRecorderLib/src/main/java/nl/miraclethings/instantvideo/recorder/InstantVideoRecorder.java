package nl.miraclethings.instantvideo.recorder;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;

import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Main recorder class
 * Created by Arjan Scherpenisse on 4-1-15.
 */
public class InstantVideoRecorder implements Camera.PreviewCallback {

    private static final String TAG = "InstantVideoRecorder";

    private final Context mContext;
    private final String mFolder;

    private String strAudioPath;
    private String strVideoPath;
    private String strFinalPath;

    private int currentResolution = Constants.RESOLUTION_MEDIUM_VALUE;

    private File fileAudioPath = null;
    private File fileVideoPath = null;

    private int sampleRate = 44100;
    private int frameRate = 30;
    private long frameTime = 0L;
    private int sampleAudioRateInHz = 44100;
    private int imageWidth = 320;
    private int imageHeight = 240;

    /* audio data getting thread */
    private AudioRecord audioRecord;
    private AudioRecordRunnable audioRecordRunnable;
    private Thread audioThread;
    volatile boolean runAudioThread = true;

    private volatile FFmpegFrameRecorder recorder;
    private long startTime;
    private boolean recording;
    private volatile long mAudioTimestamp = 0L;

    private long mLastAudioTimestamp = 0L;
    private volatile long mAudioTimeRecorded;

    private SavedFrames lastSavedframe = new SavedFrames(null, 0L);
    private long mVideoTimestamp = 0L;

//    private Camera mCamera;
//    private int mCameraId;

    private int outputWidth = 320;
    private int outputHeight = 240;

    /* The number of seconds in the continuous record loop (or 0 to disable loop). */
    final int RECORD_LENGTH = /*6*/0;
    Frame[] images;
    long[] timestamps;
    ShortBuffer[] samples;
    int imagesIndex, samplesIndex;
    private Frame yuvImage = null;

    private FFmpegFrameFilter mFilter;

    public InstantVideoRecorder(Context context, String folder/*Camera camera, */) {
        mContext = context;
        mFolder = folder;
//        mCamera = camera;

//        mCameraId = cameraId;
    }

    public boolean isRecording() {
        return recording;
    }

    /**
     * 设置图片帧的大小
     * @param width
     * @param height
     */
    public void setFrameSize(int width, int height) {
        imageWidth = width;
        imageHeight = height;
    }

    /**
     * 设置输出视频大小
     * @param width
     * @param height
     */
    public void setOutputSize(int width, int height) {
        outputWidth = width;
        outputHeight = height;
    }

    //---------------------------------------
    // initialize ffmpeg_recorder
    //---------------------------------------
    private void initRecorder() {
        Log.w(TAG, "init recorder");

        if (RECORD_LENGTH > 0) {
            imagesIndex = 0;
            images = new Frame[RECORD_LENGTH * frameRate];
            timestamps = new long[images.length];
            for (int i = 0; i < images.length; i++) {
                images[i] = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
                timestamps[i] = -1;
            }
        } else if (yuvImage == null) {
            yuvImage = new Frame(imageWidth, imageHeight, Frame.DEPTH_UBYTE, 2);
            Log.i(TAG, "create yuvImage");
        }

        RecorderParameters recorderParameters = Util.getRecorderParameter(currentResolution);
        strFinalPath = Util.createFilePath(mFolder, null, Long.toString(System.currentTimeMillis()));
//        recorder = new FFmpegFrameRecorder(strFinalPath, imageWidth, imageHeight, 1);
        // 初始化时设置录像机的目标视频大小
        recorder = new FFmpegFrameRecorder(strFinalPath, outputWidth, outputHeight, 1);
        recorder.setFormat(recorderParameters.getVideoOutputFormat());
        recorder.setSampleRate(sampleAudioRateInHz);
        // Set in the surface changed method
        recorder.setFrameRate(frameRate);

        Log.i(TAG, "recorder initialize success");

        audioRecordRunnable = new AudioRecordRunnable();
        audioThread = new Thread(audioRecordRunnable);
        runAudioThread = true;
    }

//    private void initVideoRecorder() {
//        strVideoPath = Util.createFilePath(mFolder, "tmp", Long.toString(System.currentTimeMillis()));
//        RecorderParameters recorderParameters = Util.getRecorderParameter(currentResolution);
//        fileVideoPath = new File(strVideoPath);
//        recorder = new FFmpegFrameRecorder(strVideoPath, 320, 240, 1);
//        recorder.setFormat(recorderParameters.getVideoOutputFormat());
//        recorder.setSampleRate(recorderParameters.getAudioSamplingRate());
//        recorder.setFrameRate(recorderParameters.getVideoFrameRate());
//        recorder.setVideoCodec(recorderParameters.getVideoCodec());
//        recorder.setVideoQuality(recorderParameters.getVideoQuality());
//        recorder.setAudioQuality(recorderParameters.getVideoQuality());
//        recorder.setAudioCodec(recorderParameters.getAudioCodec());
//        recorder.setVideoBitrate(1000000);
//        recorder.setAudioBitrate(64000);
//    }

    /**
     * 初始化帧过滤器
     */
    private void initFrameFilter() {
//        mFilter = new FFmpegFrameFilter(String.format("crop=w=%d:h=%d:x=0:y=0,transpose=clock", (int) (1f * outputHeight / outputWidth * imageHeight), imageHeight), imageWidth, imageHeight);
        mFilter = new FFmpegFrameFilter(String.format("crop=w=%d:h=%d:x=0:y=0,transpose=clock", (int) (1f * outputHeight / outputWidth * imageHeight), imageHeight), imageWidth, imageHeight);
        mFilter.setPixelFormat(org.bytedeco.javacpp.avutil.AV_PIX_FMT_NV21); // default camera format on Android
    }

    /**
     * 释放帧过滤器
     */
    private void releaseFrameFilter() {
        if (null != mFilter) {
            try {
                mFilter.release();
            } catch (FrameFilter.Exception e) {
                e.printStackTrace();
            }
        }
        mFilter = null;
    }

    public Uri getFilePath() {
        return Uri.fromFile(new File(strFinalPath));
    }

    /**
     * 开始录制
     * @return
     */
    public boolean startRecording() {
        boolean started = true;
        initRecorder();
        initFrameFilter();
        try {
            recorder.start();
            mFilter.start();
            startTime = System.currentTimeMillis();
            recording = true;
            audioThread.start();
        } catch (Exception e) {
            e.printStackTrace();
            started = false;
        }

        return started;
    }

    public void stopRecording() {
        if (!recording)
            return;

        runAudioThread = false;
        try {
            audioThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        audioRecordRunnable = null;
        audioThread = null;

        if (recorder != null && recording) {
            if (RECORD_LENGTH > 0) {
                Log.v(TAG,"Writing frames");
                try {
                    int firstIndex = imagesIndex % samples.length;
                    int lastIndex = (imagesIndex - 1) % images.length;
                    if (imagesIndex <= images.length) {
                        firstIndex = 0;
                        lastIndex = imagesIndex - 1;
                    }
                    if ((startTime = timestamps[lastIndex] - RECORD_LENGTH * 1000000L) < 0) {
                        startTime = 0;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += images.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        long t = timestamps[i % timestamps.length] - startTime;
                        if (t >= 0) {
                            if (t > recorder.getTimestamp()) {
                                recorder.setTimestamp(t);
                            }
                            recordFrame(images[i % images.length]);
                        }
                    }

                    firstIndex = samplesIndex % samples.length;
                    lastIndex = (samplesIndex - 1) % samples.length;
                    if (samplesIndex <= samples.length) {
                        firstIndex = 0;
                        lastIndex = samplesIndex - 1;
                    }
                    if (lastIndex < firstIndex) {
                        lastIndex += samples.length;
                    }
                    for (int i = firstIndex; i <= lastIndex; i++) {
                        recorder.recordSamples(samples[i % samples.length]);
                    }
                } catch (Exception e) {
                    Log.v(TAG,e.getMessage());
                    e.printStackTrace();
                }
            }

            recording = false;
            Log.v(TAG,"Finishing recording, calling stop and release on recorder");
            try {
                recorder.stop();
                recorder.release();
            } catch (FFmpegFrameRecorder.Exception e) {
                e.printStackTrace();
            }
            recorder = null;

            // 释放帧过滤器
            releaseFrameFilter();
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        try {
            if (audioRecord == null || audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                startTime = System.currentTimeMillis();
                return;
            }
            if (RECORD_LENGTH > 0) {
                int i = imagesIndex++ % images.length;
                yuvImage = images[i];
                timestamps[i] = 1000 * (System.currentTimeMillis() - startTime);
            }
            /* get video data */
            if (yuvImage != null && recording) {
                ((ByteBuffer) yuvImage.image[0].position(0)).put(data);

                if (RECORD_LENGTH <= 0) try {
                    Log.v(TAG, "Writing Frame");
                    long t = 1000 * (System.currentTimeMillis() - startTime);
                    if (t > recorder.getTimestamp()) {
                        recorder.setTimestamp(t);
                    }
                    recordFrame(yuvImage);
                } catch (Exception e) {
                    Log.v(TAG, e.getMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            camera.addCallbackBuffer(data);
        }
    }

    /**
     * 录制帧
     * @throws FrameRecorder.Exception
     */
    private void recordFrame(Frame frame) throws FrameRecorder.Exception, FrameFilter.Exception {
        mFilter.push(frame);
        Frame filteredFrame;
        while ((filteredFrame = mFilter.pull()) != null) {
            recorder.record(filteredFrame);
        }
    }

    //---------------------------------------------
    // audio thread, gets and encodes audio data
    //---------------------------------------------
    class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            if (RECORD_LENGTH > 0) {
                samplesIndex = 0;
                samples = new ShortBuffer[RECORD_LENGTH * sampleAudioRateInHz * 2 / bufferSize + 1];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = ShortBuffer.allocate(bufferSize);
                }
            } else {
                audioData = ShortBuffer.allocate(bufferSize);
            }

            Log.d(TAG, "audioRecord.startRecording()");
            audioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (runAudioThread) {
                if (RECORD_LENGTH > 0) {
                    audioData = samples[samplesIndex++ % samples.length];
                    audioData.position(0).limit(0);
                }
                //Log.v(LOG_TAG,"recording? " + recording);
                bufferReadResult = audioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
                    Log.v(TAG,"bufferReadResult: " + bufferReadResult);
                    // If "recording" isn't true when start this thread, it never get's set according to this if statement...!!!
                    // Why?  Good question...
                    if (recording) {
                        if (RECORD_LENGTH <= 0) try {
                            recorder.recordSamples(audioData);
                            //Log.v(LOG_TAG,"recording " + 1024*i + " to " + 1024*i+1024);
                        } catch (FFmpegFrameRecorder.Exception e) {
                            Log.v(TAG,e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
            Log.v(TAG,"AudioThread Finished, release audioRecord");

            /* encoding finish, release recorder */
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
                Log.v(TAG,"audioRecord released");
            }
        }
    }

    public void releaseResources() {

        if (fileAudioPath != null && fileAudioPath.exists())
            fileAudioPath.delete();
        if (fileVideoPath != null && fileVideoPath.exists())
            fileVideoPath.delete();

        recording = false;
        try {
            if (recorder != null) {
                recorder.release();
            }
            if (recorder != null) {
                recorder.release();
            }
        } catch (FrameRecorder.Exception e) {
            e.printStackTrace();
        }

        recorder = null;
        recorder = null;
        lastSavedframe = null;
    }


}
