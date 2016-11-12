using UnityEngine;

public static class GameUtils
{
    public static void PruneMiddle(GameObject container, int x, int y)
    {
        OverlapWFC WFC = container.GetComponent<OverlapWFC>();
        int width = WFC.rendering.GetLength(0);
        int height = WFC.rendering.GetLength(1);
        
        for (int i = width / 2 - x / 2; i < width / 2 + x / 2; i++)
        {
            for (int j = height / 2 - y / 2; j < height / 2 + y / 2; j++)
            {
                GameObject tile = WFC.rendering[i, j];
                GameObject.Destroy(tile);
            }
        }
    }

    static string AppendTimeStamp(this string fileName)
    {
        return string.Concat(
            System.IO.Path.GetFileNameWithoutExtension(fileName),
            System.DateTime.Now.ToString("yyyyMMddHHmmssfff"),
            System.IO.Path.GetExtension(fileName)
        );
    }

    static void CreateDocumentDir(string folderName)
    {
        System.IO.Directory.CreateDirectory(GetDocumentsDir() + "//" + folderName.Trim());
    }

    public static string CreateDocumentFile()
    {
        CreateDocumentDir("procadia");

        string fileName = AppendTimeStamp("procadia");
        string fullPath = GetDocumentsDir() + "//procadia//" + fileName + ".gif";
        var file = System.IO.File.Create(fullPath);
        file.Close();

        return fullPath;
    }

    static string GetDocumentsDir()
    {
        return System.Environment.GetFolderPath(System.Environment.SpecialFolder.MyDocuments);
    }
}