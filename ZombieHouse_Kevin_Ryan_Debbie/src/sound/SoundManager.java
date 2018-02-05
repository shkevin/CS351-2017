package sound;

import game_engine.Attributes;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Ben Matthews
 *         Nick Schrandt
 *         <p>
 *         This class handles all of the sound loading and playing for the game
 */
public class SoundManager
{
  private final int DEATHS = 2;
  private final int FOOTSTEPS = 10;
  private final int GROANS = 4;
  private final int SHUFFLES = 4;

  //volume modifiers
  private static final double ACHIEVE_MOD = 1;
  private static final double BUTTON_MOD = .1;
  private static final double DEATH_MOD = 1;
  private static final double FOOTSTEP_MOD = .4;
  private static final double SHUFFLE_MOD = .4;
  private static final double GROAN_MOD = .4;

  private Map<String, AudioClip> sounds;
  private ArrayList<Media> tracks;

  private MediaPlayer mediaPlayer;
  private boolean paused = false;

  /**
   * Default constructor
   */
  public SoundManager()
  {
    sounds = new HashMap<>();
    tracks = new ArrayList<>();

    try
    {
      init();
    }
    catch (MalformedURLException e)
    {
      e.printStackTrace();
    }
  }

  /**
   * @throws MalformedURLException This method initializes the SoundManager and reads in the sound
   *                               clip files
   */
  private void init() throws MalformedURLException
  {
    String name = "";

    URL url;

    //achievements:
    name = "exit";
    url = getClass().getResource("/Sounds/achievements/" + name + ".wav");
    loadSoundClip(name, url);

    name = "Strong_Punch";
    url = getClass().getResource("/Sounds/weapon/" + name + ".wav");
    loadSoundClip(name, url);

    //deaths:
    name = "death";
    for (int i = 0; i < DEATHS; i++)
    {
      url = getClass().getResource("/Sounds/deaths/" + name + i + ".wav");
      loadSoundClip(name + i, url);
    }

    //footsteps:
    name = "footstep";
    for (int i = 0; i < FOOTSTEPS; i++)
    {
      url = getClass().getResource("/Sounds/footsteps/" + name + i + ".wav");
      loadSoundClip(name + i, url);
    }

    //groans:
    name = "groan";
    for (int i = 0; i < GROANS; i++)
    {
      url = getClass().getResource("/Sounds/groans/" + name + i + ".wav");
      loadSoundClip(name + i, url);
    }

    //gui:
    name = "button";
    url = getClass().getResource("/Sounds/gui/" + name + ".wav");
    loadSoundClip(name, url);

    //shuffles:
    name = "shuffle";
    for (int i = 0; i < SHUFFLES; i++)
    {
      url = getClass().getResource("/Sounds/shuffles/" + name + i + ".wav");
      loadSoundClip(name + i, url);
    }

    //main song track
    name = "Paranormal_Activity";
    url = getClass().getResource("/Sounds/music/" + name + ".mp3");
    loadTrack(url);

    //Nick: Sword hit
    name = "sword_hit";
    url = getClass().getResource("/Sounds/weapon/" + name + ".wav");
    loadSoundClip(name, url);

    //Nick: sword SWING
    name = "sword_swing";
    url = getClass().getResource("/Sounds/weapon/" + name + ".mp3");
    loadSoundClip(name, url);

    //Nick: Sword PUSH
    name = "push";
    url = getClass().getResource("/Sounds/weapon/" + name + ".wav");
    loadSoundClip(name, url);

    //Nick: player GRUNT
    name = "player_grunt";
    url = getClass().getResource("/Sounds/groans/" + name + ".wav");
    loadSoundClip(name, url);
  }

  /**
   * loads a sound from a specific URL, usually a file in Resources/Sounds
   *
   * @param ID
   * @param url
   */
  private void loadSoundClip(String ID, URL url)
  {
    AudioClip sound = new AudioClip(url.toExternalForm());
    sounds.put(ID, sound);
  }

  /**
   * loads a song track from a specific url, usually a file in Resources/Sounds
   *
   * @param url
   */
  private void loadTrack(URL url)
  {
    Media track = new Media(url.toString());
    tracks.add(track);
  }

  /**
   * plays a random soundclip of a given type defined by the Sound enum.
   * the sound will be played at the specified balance, volume, and pan<br><br>
   * <p>
   * should be used for zombie sound
   *
   * @param sound
   */
  public void playSoundClip(Sound sound, double distance, double balance)
  {
    Random random = new Random();
    double volume = 1;
    AudioClip clip = null;

    double hearing = Attributes.Player_Hearing;
    distance = (hearing - distance) / hearing;
    if (distance < 0) distance = 0;

    //GROAN
    if (sound.equals(Sound.GROAN))
    {
      int i = random.nextInt(GROANS);
      clip = sounds.get("groan" + i);
      volume = GROAN_MOD * distance;
    }

    //SHUFFLE
    if (sound.equals(Sound.SHUFFLE))
    {
      int i = random.nextInt(SHUFFLES);
      clip = sounds.get("shuffle" + i);
      volume = SHUFFLE_MOD * distance;
    }

    playSound(clip, volume, balance);
  }

  /**
   * This Method plays a sound clip of the given type specified by Sound
   * and plays that sound clip using the default volume modifier
   *
   * @param sound - the type of sound to be played
   */
  public void playSoundClip(Sound sound)
  {
    Random random = new Random();
    double volume = 1;
    double balance = 0;
    AudioClip clip = null;

    //ACHIEVE
    if (sound.equals(Sound.ACHIEVE))
    {
      clip = sounds.get("exit");
      volume = ACHIEVE_MOD;
    }

    //DEATH
    if (sound.equals(Sound.DEATH))
    {
      int i = random.nextInt(DEATHS);
      clip = sounds.get("death" + i);
      volume = DEATH_MOD;
    }

    //FOOTSTEP
    if (sound.equals(Sound.FOOTSTEP))
    {
      int i = random.nextInt(FOOTSTEPS);
      clip = sounds.get("footstep" + i);
      volume = FOOTSTEP_MOD;
    }

    //gui
    if (sound.equals(Sound.BUTTON))
    {
      clip = sounds.get("button");
      volume = BUTTON_MOD;
    }

    //@Nick: sword SWING
//    if (sound.equals(Sound.SWING))
//    {
//      clip = sounds.get("sword_swing");
//      volume = FOOTSTEP_MOD;
//    }

    //@Nick: Sword hits zombie
    if (sound.equals(Sound.PUNCH))
    {
      clip = sounds.get("Strong_Punch");
      volume = FOOTSTEP_MOD;
    }

    //@Nick: Push zombie
    if (sound.equals(Sound.PUSH))
    {
      clip = sounds.get("push");
      volume = FOOTSTEP_MOD;
    }

    //@Nick: player GRUNT
    if (sound.equals(Sound.GRUNT))
    {
      clip = sounds.get("player_grunt");
      volume = FOOTSTEP_MOD;
    }
    playSound(clip, volume, balance);
  }

  /**
   * This method takes a sound clip and plays
   * that sound clip at the given volume and balance
   *
   * @param clip    - the clip to be played
   * @param volume  - the volume to play the clip at
   * @param balance - the balance to play the clip at
   */
  private void playSound(AudioClip clip, double volume, double balance)
  {
    if (clip.equals(null)) return;

    clip.play(volume, balance, 1, 0, 1);
  }

  /**
   * plays the given track based off of the track number
   *
   * @param trackNumber
   */
  public void playTrack(int trackNumber)
  {
    if (trackNumber < tracks.size())
    { //catch out of bounds
      if (mediaPlayer != null) mediaPlayer.dispose();
      mediaPlayer = new MediaPlayer(tracks.get(trackNumber));
      mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
      mediaPlayer.setVolume(.05);
      mediaPlayer.play();
    }
  }

  /**
   * pauses all the audio in the game
   */
  private void pause()
  {
    if (!paused)
    {
      paused ^= true;
      for (AudioClip sound : sounds.values())
      {
        sound.stop();
      }
      mediaPlayer.pause();
    }
  }

  /**
   * stops all of the audio and disposes
   */
  public void stop()
  {
    pause();
    stopTrack();
  }

  /**
   * stops the mediaPlayer
   */
  public void stopTrack()
  {
    mediaPlayer.stop();
  }

  /**
   * resumes playing the audio in the game
   */
  public void play()
  {
    if (paused)
    {
      paused ^= true;
      mediaPlayer.play();
    }
  }
}