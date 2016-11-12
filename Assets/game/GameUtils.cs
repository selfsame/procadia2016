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
}