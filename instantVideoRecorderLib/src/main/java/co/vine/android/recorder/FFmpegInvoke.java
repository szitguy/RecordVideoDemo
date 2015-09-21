package co.vine.android.recorder;




public class FFmpegInvoke
{
  private String mPath;

  public FFmpegInvoke(String paramString)
  {
    this.mPath = paramString;
  }

  private native int run(String paramString, String[] paramArrayOfString);

  public native void YUVtoRBG(int[] paramArrayOfInt, byte[] paramArrayOfByte, int paramInt1, int paramInt2);

  public int run(String[] paramArrayOfString)
  {
	  int res = -1;
	  try
	  {
	  res = run(this.mPath, paramArrayOfString);
	  }
	  catch(Exception e)
	  {
		  e.printStackTrace();
		  return res;
	  }
    return res;
  }
}