using UnityEngine;
using UnityEditor;
using System;
using System.IO;
using Newtonsoft.Json;

public static class AutoBuilder
{
    static string build_version = string.Empty;

    static string GetProjectName ()
	{
		string[] s = Application.dataPath.Split ('/');
		return s [s.Length - 2];
	}

	static string[] GetScenePaths ()
	{
		string[] scenes = new string[EditorBuildSettings.scenes.Length];

		for (int i = 0; i < scenes.Length; i++) {
			scenes [i] = EditorBuildSettings.scenes [i].path;
		}

		return scenes;
	}

	[MenuItem ("File/AutoBuilder/android")]
	public static void PerformAndroidBuild ()
	{
		var args = Environment.GetCommandLineArgs ();
        Console.WriteLine (args.ToString ());
        
        string build_config_file_path = Environment.CurrentDirectory + "/ProjectSettings/build_config.json";		
        string content = File.ReadAllText(build_config_file_path);
        BuildConfig config = JsonConvert.DeserializeObject<BuildConfig>(content);
        build_version = config.BuildVersion;
        PlayerSettings.bundleVersion = build_version;

        EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTarget.Android);
        BuildPipeline.BuildPlayer(GetScenePaths(), "Builds/android/ApplicationName-" + build_version + ".apk", BuildTarget.Android, BuildOptions.None);
    }

	[MenuItem ("File/AutoBuilder/iOS")]
	static void PerformiOSBuild ()
	{
		var args = Environment.GetCommandLineArgs ();
        Console.WriteLine (args.ToString ());
		
		EditorUserBuildSettings.SwitchActiveBuildTarget (BuildTarget.iOS);
		BuildPipeline.BuildPlayer (GetScenePaths (), "Builds/iOS", BuildTarget.iOS, BuildOptions.None);
	}

}


public class BuildConfig
{
    public string BuildVersion { get; set; }
}

