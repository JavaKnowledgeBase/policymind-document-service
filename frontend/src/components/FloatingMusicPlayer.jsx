import { useCallback, useEffect, useMemo, useRef, useState } from "react";

const PLAYLIST = [
  { title: "Song 1", file: "/music/song1.mp3" },
  { title: "Song 2", file: "/music/song2.mp3" },
  { title: "Song 3", file: "/music/song3.mp3" }
];

function nextTrackIndex(current, length, shuffle) {
  if (length <= 1) {
    return current;
  }
  if (!shuffle) {
    return (current + 1) % length;
  }

  let candidate = current;
  while (candidate === current) {
    candidate = Math.floor(Math.random() * length);
  }
  return candidate;
}

export default function FloatingMusicPlayer() {
  const audioRef = useRef(null);
  const hideTimerRef = useRef(null);
  const [trackIndex, setTrackIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [showPlaylist, setShowPlaylist] = useState(false);
  const [volume, setVolume] = useState(0.2);
  const [shuffle, setShuffle] = useState(false);
  const [loop, setLoop] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");
  const currentTrack = useMemo(() => PLAYLIST[trackIndex], [trackIndex]);

  const playTrack = useCallback(
    async (index, forceReload = false) => {
      const audio = audioRef.current;
      if (!audio) {
        return;
      }

      const track = PLAYLIST[index];
      if (!track) {
        return;
      }

      const resolvedTrackUrl = new URL(track.file, window.location.origin).href;
      if (forceReload || audio.src !== resolvedTrackUrl) {
        audio.src = track.file;
      }

      try {
        await audio.play();
        setIsPlaying(true);
        setStatusMessage("");
      } catch (error) {
        setIsPlaying(false);
        setStatusMessage("Autoplay blocked or missing music file.");
      }
    },
    []
  );

  const pauseTrack = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) {
      return;
    }
    audio.pause();
    setIsPlaying(false);
  }, []);

  const togglePlayPause = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) {
      return;
    }

    if (audio.paused) {
      playTrack(trackIndex);
      return;
    }
    pauseTrack();
  }, [pauseTrack, playTrack, trackIndex]);

  useEffect(() => {
    const audio = audioRef.current;
    if (!audio) {
      return undefined;
    }

    audio.volume = volume;

    const onEnded = () => {
      if (loop) {
        playTrack(trackIndex, true);
        return;
      }
      setTrackIndex((prev) => nextTrackIndex(prev, PLAYLIST.length, shuffle));
    };

    const onPlay = () => setIsPlaying(true);
    const onPause = () => setIsPlaying(false);

    audio.addEventListener("ended", onEnded);
    audio.addEventListener("play", onPlay);
    audio.addEventListener("pause", onPause);

    return () => {
      audio.removeEventListener("ended", onEnded);
      audio.removeEventListener("play", onPlay);
      audio.removeEventListener("pause", onPause);
    };
  }, [loop, playTrack, shuffle, trackIndex, volume]);

  useEffect(() => {
    playTrack(trackIndex, true);
  }, [playTrack, trackIndex]);

  useEffect(() => {
    return () => {
      if (hideTimerRef.current) {
        clearTimeout(hideTimerRef.current);
      }
    };
  }, []);

  const openPlaylist = () => {
    if (hideTimerRef.current) {
      clearTimeout(hideTimerRef.current);
    }
    setShowPlaylist(true);
  };

  const closePlaylistSoon = () => {
    hideTimerRef.current = setTimeout(() => {
      setShowPlaylist(false);
    }, 500);
  };

  return (
    <div className="music-face-player" onMouseEnter={openPlaylist} onMouseLeave={closePlaylistSoon}>
      <svg className="music-face-svg" viewBox="0 0 100 100" aria-label="Music player face">
        <circle cx="50" cy="50" r="45" fill="#FFD966" />
        <circle cx="35" cy="40" r="5" fill="#111827" />
        <circle cx="65" cy="40" r="5" fill="#111827" />
        <circle
          className="ear-control"
          cx="85"
          cy="50"
          r="7"
          onMouseEnter={openPlaylist}
          role="button"
          aria-label="Show playlist"
        />
        <rect
          className={`mouth-control ${isPlaying ? "mouth-open" : ""}`}
          x="34"
          y="64"
          width="32"
          height="11"
          rx="6"
          onClick={togglePlayPause}
          role="button"
          aria-label={isPlaying ? "Pause music" : "Play music"}
        />
      </svg>

      {showPlaylist && (
        <div className="playlist-box">
          <p className="playlist-title">{currentTrack?.title || "Playlist"}</p>
          <div className="playlist-controls">
            <button type="button" onClick={() => playTrack(trackIndex)}>
              Play
            </button>
            <button type="button" onClick={pauseTrack}>
              Pause
            </button>
            <button
              type="button"
              className={shuffle ? "active-control" : ""}
              onClick={() => setShuffle((value) => !value)}
            >
              Shuffle
            </button>
            <button
              type="button"
              className={loop ? "active-control" : ""}
              onClick={() => setLoop((value) => !value)}
            >
              Loop
            </button>
          </div>
          <label className="volume-control" htmlFor="playlist-volume">
            Volume
            <input
              id="playlist-volume"
              type="range"
              min="0"
              max="1"
              step="0.05"
              value={volume}
              onChange={(event) => setVolume(Number(event.target.value))}
            />
          </label>
          <ul>
            {PLAYLIST.map((track, index) => (
              <li key={track.title}>
                <button
                  type="button"
                  className={index === trackIndex ? "track-active" : ""}
                  onClick={() => setTrackIndex(index)}
                >
                  {track.title}
                </button>
              </li>
            ))}
          </ul>
          {statusMessage && <p className="playlist-status">{statusMessage}</p>}
        </div>
      )}

      <audio ref={audioRef} preload="none" />
    </div>
  );
}
