using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class SphereBehaviourScript : MonoBehaviour {

    private AudioSource aud;
    public string micName = null;
    private Renderer rend;  // for changing color
    public Color colorStart = Color.blue;
    public Color colorEnd = Color.red;

    // Use this for initialization
    void Start () {

        // print out which mic devices are available
        foreach (string device in Microphone.devices)
        {
            Debug.Log("Device Name: " + device);
            if (micName == null)
            {
                micName = device;
            }
        }
        aud = GetComponent<AudioSource>();
        rend = GetComponent<Renderer>();

        // get the mic going
        aud.Stop();
        aud.clip = Microphone.Start(micName, true, 10, 44100);
        aud.loop = true;
        if (Microphone.IsRecording(micName))
        {
            while (!(Microphone.GetPosition(micName) > 0))
            {
            } // just hang out until mic is actually recording
            Debug.Log("We are now recording using " + micName);
        }
        else
        {
            Debug.Log("Error: We are not able to record!");
        }
        aud.Play();  // start 'playing' from this mic source

    }

    // Update is called once per frame
    void Update () {

        // find out what the mic volume is
        float vol = GetAveragedVolume();
        float volThresh = 0.006f;
        float sizeSens = 2;                 // size sensitivity
        //Debug.Log("vol = " + vol);

        // rescale the object accordingly
        Vector3 baseScale;
        baseScale.x = 0.05f;
        baseScale.y = baseScale.x;
        baseScale.z = baseScale.x;
        Vector3 scale = transform.localScale;
        scale.x = 0.0f + vol*sizeSens;
        scale.y = scale.x;
        scale.z = scale.x;
        transform.localScale = scale;
        if (vol < volThresh)
        {
            transform.localScale *= 0;
        } else
        {
            transform.localScale = baseScale;
        }
       


        // change its color
        float colorSens = 10;
        float lerp = vol * colorSens;
        rend.material.color = Color.Lerp(colorStart, colorEnd, lerp);



    }



    public float GetAveragedVolume()
    {
        int sampleLength = 1024;     // amount of samples to average over 
        float[] data = new float[sampleLength];
        float a = 0.0f;
        aud.GetOutputData(data, 0);
        foreach (float s in data)
        {
            a += Mathf.Abs(s);
        }
        return a / sampleLength;
    }




}
