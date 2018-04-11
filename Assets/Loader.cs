using UnityEngine;
using UnityEngine.SceneManagement;
using System.Collections;


public class Loader : MonoBehaviour{
	public int delay = 20;
	public string scene;
	public void Start(){

	}

	public void Update(){
		delay -= 1;
		if (delay == 0){
			GameObject instance = Instantiate(Resources.Load("requires", typeof(GameObject))) as GameObject;
			SceneManager.LoadScene(scene);
		}
	}


}
