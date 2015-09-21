package nl.miraclethings.instantvideo.recorder;




import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Pattern;

import co.vine.android.recorder.FFmpegInvoke;

public final class Processor
{
  private ArrayList<String> mCommand;
  private ArrayList<String> mFilters;
  private final FFmpegInvoke mInvoker;
  private HashMap<String, String> mMetaData;
  private final int mNumCores;
  Context context = null;
  static
  {
    System.loadLibrary("ffmpeginvoke");
  }

  public Processor(String paramString, Context context)
  {
    this.mInvoker = new FFmpegInvoke(paramString);
    this.mNumCores = getNumCores();
    this.context = context;
  }

  private static int getNumCores()
  {
    try
    {
      int i = new File("/sys/devices/system/cpu/").listFiles(new FileFilter()
      {
        public boolean accept(File paramAnonymousFile)
        {
          return Pattern.matches("cpu[0-9]", paramAnonymousFile.getName());
        }
      }).length;
      return i;
    }
    catch (Exception localException)
    {
    }
    return 1;
  }

  public Processor addInputPath(String paramString)
  {
    this.mCommand.add("-i");
    this.mCommand.add(paramString);
    return this;
  }

  public Processor addMetaData(String paramString1, String paramString2)
  {
    this.mMetaData.put(paramString1, paramString2);
    return this;
  }

  public Processor enableOverwrite()
  {
    this.mCommand.add("-y");
    return this;
  }

  public Processor enableShortest()
  {
    this.mCommand.add("-shortest");
    return this;
  }

  public Processor filterCrop(int paramInt1, int paramInt2)
  {
    this.mFilters.add("crop=" + paramInt1 + ":" + paramInt2);
    return this;
  }

 public Processor newCommand()
  {
    this.mMetaData = new HashMap();
    this.mFilters = new ArrayList();
    this.mCommand = new ArrayList();
   this.mCommand.add("ffmpeg");

    return this;
  }

  public int process(String[] paramArrayOfString)
  {
    return this.mInvoker.run(paramArrayOfString);
  }

  public int processToOutput(String paramString)
  {
    if (this.mFilters.size() > 0)
    {
      this.mCommand.add("-vf");
      StringBuilder localStringBuilder = new StringBuilder();
      Iterator localIterator3 = this.mFilters.iterator();
      while (localIterator3.hasNext())
      {
        localStringBuilder.append((String)localIterator3.next());
        localStringBuilder.append(",");
      }
      String str2 = localStringBuilder.toString();
      this.mCommand.add(str2.substring(0, -1 + str2.length()));
    }
    Iterator localIterator1 = this.mMetaData.keySet().iterator();
    while (localIterator1.hasNext())
    {
      String str1 = (String)localIterator1.next();
      this.mCommand.add("-metadata");
      this.mCommand.add(str1 + "=" + "\"" + (String)this.mMetaData.get(str1) + "\"");
    }
    if (this.mNumCores > 1);
    this.mCommand.add(paramString);
    Iterator localIterator2 = this.mCommand.iterator();
    if (localIterator2.hasNext())
      Log.i("Add arg '{}'", (String)localIterator2.next());
    return process((String[])this.mCommand.toArray(new String[this.mCommand.size()]));
  }

  public Processor setOrientation(int orientation) {
    this.mCommand.add("-metadata:s:v:0");
    this.mCommand.add("rotate=" + Integer.toString(orientation));
    return this;
  }

  public Processor setAudioCopy()
  {
    this.mCommand.add("-acodec");
    this.mCommand.add("copy");
    return this;
  }

  public Processor setBsfA(String paramString)
  {
    this.mCommand.add("-bsf:a");
    this.mCommand.add(paramString);
    return this;
  }

  public Processor setBsfV(String paramString)
  {
    this.mCommand.add("-bsf:v");
    this.mCommand.add(paramString);
    return this;
  }

  public Processor setCopy()
  {
    this.mCommand.add("-c");
    this.mCommand.add("copy");
    return this;
  }

  public Processor setFormat(String paramString)
  {
    this.mCommand.add("-f");
    this.mCommand.add(paramString);
    return this;
  }

  public Processor setFrames(long paramLong, int paramInt)
  {
    this.mCommand.add("-vframes");
    this.mCommand.add(String.valueOf((int)(paramLong / 1000.0D * paramInt)));
    return this;
  }

  public Processor setMap(String paramString)
  {
    this.mCommand.add("-map");
    this.mCommand.add(paramString);
    return this;
  }

  public Processor setMetaData(HashMap<String, String> paramHashMap)
  {
    this.mMetaData = paramHashMap;
    return this;
  }

 public Processor setShortest()
  {
    this.mCommand.add("-shortest");
    return this;
  }

  public Processor setStart(long paramLong)
  {
    this.mCommand.add("-ss");
    this.mCommand.add(String.valueOf(paramLong / 1000.0D));
    return this;
  }

  public Processor setTotalDuration(long paramLong)
  {
    this.mCommand.add("-t");
    this.mCommand.add(String.valueOf(paramLong / 1000.0D));
    return this;
  }

  public Processor setVideoCopy()
  {
    this.mCommand.add("-vcodec");
    this.mCommand.add("copy");
    return this;
  }

  public Processor useX264()
  {
    this.mCommand.add("-vcodec");
    this.mCommand.add("libx264");
    return this;
  }
}