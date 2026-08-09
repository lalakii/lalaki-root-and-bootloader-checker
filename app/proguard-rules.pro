-libraryjars "C:\Users\i\AppData\Local\Android\Sdk\platforms\android-37.1\android.jar"
-optimizationpasses 7
-dontusemixedcaseclassnames
-overloadaggressively
-repackageclasses "c"
-adaptresourcefilenames
-mergeinterfacesaggressively
-allowaccessmodification
-dontwarn java.lang.**
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static *** isLoggable(java.lang.String, ...);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
    public static java.lang.String getStackTraceString(java.lang.Throwable);}
-renamesourcefileattribute ''
-ignorewarnings
-verbose