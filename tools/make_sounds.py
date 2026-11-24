"""Builds every sound in the mod as mono Ogg Vorbis, plus sounds.json.

Two kinds of sound:
  * synthesized here from scratch (drones, horns, impacts, chimes): ours, MIT like the code
  * CC0 recordings from freesound.org in tools/sounds_raw, trimmed and treated here: see SOUND-CREDITS.md

Run from the project root:  python tools/make_sounds.py      (needs numpy, scipy, soundfile)
"""
import json
import os
from fractions import Fraction

import numpy as np
import soundfile as sf
from scipy import signal

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "sounds_raw")
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "fromthemist")
OUT = os.path.join(ASSETS, "sounds")
rng = np.random.default_rng(7)


# ------------------------------------------------------------------ building blocks
def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(seconds):
    return rng.standard_normal(int(SR * seconds))


def lowpass(x, hz, order=4):
    return signal.sosfilt(signal.butter(order, hz, "low", fs=SR, output="sos"), x)


def highpass(x, hz, order=2):
    return signal.sosfilt(signal.butter(order, hz, "high", fs=SR, output="sos"), x)


def bandpass(x, lo, hi, order=2):
    return signal.sosfilt(signal.butter(order, [lo, hi], "band", fs=SR, output="sos"), x)


def fade(x, fade_in=0.01, fade_out=0.05):
    x = x.copy()
    a, b = int(SR * fade_in), int(SR * fade_out)
    if a:
        x[:a] *= np.linspace(0.0, 1.0, a)
    if b:
        x[-b:] *= np.linspace(1.0, 0.0, b)
    return x


def normalize(x, peak=0.89):
    return x * (peak / max(1e-9, np.abs(x).max()))


def reverb(x, seconds=1.2, mix=0.3, tone=2500):
    """A cheap hall: convolve with decaying filtered noise."""
    n = int(SR * seconds)
    tail = lowpass(rng.standard_normal(n), tone) * np.exp(-np.arange(n) / (SR * seconds / 5.0))
    wet = signal.fftconvolve(np.concatenate([x, np.zeros(n)]), tail)[: len(x) + n]
    wet *= np.abs(x).max() / max(1e-9, np.abs(wet).max())
    dry = np.concatenate([x, np.zeros(n)])
    return dry * (1.0 - mix) + wet * mix


def saw(freq, harmonics=24):
    """Band-limited sawtooth for a frequency curve (array, Hz)."""
    phase = 2.0 * np.pi * np.cumsum(freq) / SR
    out = np.zeros_like(phase)
    for k in range(1, harmonics + 1):
        out += np.sin(k * phase) / k * (np.max(freq) * k < SR / 2.2)
    return out


def sine(freq):
    return np.sin(2.0 * np.pi * np.cumsum(freq) / SR)


def envelope(n, attack, release, hold=1.0):
    """Rise over `attack` seconds, hold, fall over `release` seconds."""
    env = np.full(n, hold)
    a, r = int(SR * attack), int(SR * release)
    env[:a] = np.linspace(0.0, hold, a) ** 1.5
    env[n - r:] = np.linspace(hold, 0.0, r) ** 1.5
    return env


def thump(seconds, start_hz, end_hz, decay):
    x = t(seconds)
    freq = end_hz + (start_hz - end_hz) * np.exp(-x * 9.0)
    return sine(freq) * np.exp(-x / decay)


def save(name, x, peak=0.89):
    os.makedirs(OUT, exist_ok=True)
    data = normalize(x, peak).astype(np.float32)
    # Written in small blocks: libsndfile's Vorbis encoder crashes when handed a long buffer in one call.
    with sf.SoundFile(os.path.join(OUT, name + ".ogg"), "w", SR, 1, format="OGG", subtype="VORBIS") as out:
        for start in range(0, len(data), 8192):
            out.write(data[start:start + 8192])
    print(f"  {name:22s} {len(x) / SR:5.1f}s", flush=True)


# ------------------------------------------------------------------ synthesized
def mist_loop():
    """18 seconds of low wind and a beating drone, faded both ends so overlapping plays cross-fade."""
    seconds = 18.0
    x = t(seconds)
    wind = bandpass(noise(seconds), 120, 520) * (0.55 + 0.45 * np.sin(2 * np.pi * x / 9.0) * np.sin(2 * np.pi * x / 3.7 + 1.0))
    air = bandpass(noise(seconds), 1800, 5200) * 0.05 * (0.5 + 0.5 * np.sin(2 * np.pi * x / 6.0 + 2.0))
    drone = (np.sin(2 * np.pi * 49.0 * x) + 0.8 * np.sin(2 * np.pi * 49.6 * x) + 0.5 * np.sin(2 * np.pi * 73.4 * x + 1.0)) * 0.16
    return fade(wind * 0.7 + air + drone, 3.0, 3.0)


def mist_horn():
    """The warning when a bank forms: two low tones a semitone apart, swelling and dying, a long way off."""
    seconds = 6.5
    n = int(SR * seconds)
    x = t(seconds)
    vibrato = 1.0 + 0.004 * np.sin(2 * np.pi * 4.3 * x)
    a = saw(np.full(n, 87.3) * vibrato)
    b = saw(np.full(n, 92.5) * vibrato) * envelope(n, 2.5, 2.0)
    tone = lowpass((a + 0.7 * b) * envelope(n, 0.9, 2.6), 520)
    return reverb(tone, 2.4, 0.45, 1400)


def colossus_call():
    """A foghorn with something alive in it: a very low reed tone that sags in pitch and growls."""
    seconds = 5.5
    n = int(SR * seconds)
    x = t(seconds)
    freq = 58.0 - 7.0 * np.clip((x - 3.2) / 2.3, 0.0, 1.0) ** 2 + 1.5 * np.sin(2 * np.pi * 0.5 * x)
    growl = 1.0 + 0.35 * np.sin(2 * np.pi * 17.0 * x) * np.clip(x / 2.0, 0.0, 1.0)
    tone = lowpass(saw(freq) * growl, 380) + 0.5 * sine(freq / 2.0)
    return reverb(tone * envelope(n, 0.7, 2.2), 2.6, 0.4, 900)


def strider_call():
    """Higher and more animal: a moan that rises, falls and flutters."""
    seconds = 3.6
    n = int(SR * seconds)
    x = t(seconds)
    freq = 150.0 + 55.0 * np.sin(np.pi * x / seconds) ** 2 - 35.0 * (x / seconds) ** 2
    flutter = 1.0 + 0.3 * np.sin(2 * np.pi * (9.0 + 6.0 * x / seconds) * x)
    tone = lowpass(saw(freq, 16) * flutter, 900) + 0.4 * sine(freq * 0.5)
    return reverb(tone * envelope(n, 0.5, 1.4), 2.0, 0.45, 1600)


def footfall():
    """Something enormous putting its weight down: a sub thump, a body of low noise, loose debris after."""
    seconds = 2.2
    n = int(SR * seconds)
    x = t(seconds)
    body = lowpass(noise(seconds), 160) * np.exp(-x / 0.28)
    debris = bandpass(noise(seconds), 400, 3000) * np.exp(-np.clip(x - 0.06, 0, None) / 0.22) * (x > 0.06) * 0.12
    debris *= rng.random(n) > 0.6
    return fade(thump(seconds, 85.0, 30.0, 0.5) * 1.0 + body * 0.8 + debris, 0.002, 0.3)


def crack():
    """A joint giving way: two or three sharp dry breaks."""
    seconds = 0.7
    x = t(seconds)
    out = np.zeros(len(x))
    for at, level in ((0.0, 1.0), (0.11, 0.6), (0.19, 0.8)):
        hit = bandpass(noise(seconds), 300, 2600) * np.exp(-np.clip(x - at, 0, None) / 0.035) * (x >= at)
        out += hit * level
    return fade(out + thump(seconds, 140.0, 60.0, 0.08) * 0.6, 0.001, 0.1)


def crash():
    """The body hitting the ground: a long boom, a wall of rubble, a low rumble that takes seconds to settle."""
    seconds = 4.5
    n = int(SR * seconds)
    x = t(seconds)
    boom = thump(seconds, 70.0, 24.0, 1.1)
    rubble = bandpass(noise(seconds), 150, 2400) * np.exp(-x / 0.7) * 0.5
    rumble = lowpass(noise(seconds), 70) * np.exp(-x / 1.6) * 1.2
    return reverb(fade(boom + rubble + rumble, 0.002, 0.6), 1.8, 0.25, 1200)


def chime(base, seconds, partials=((1.0, 1.0), (2.76, 0.5), (5.4, 0.25), (8.93, 0.12))):
    x = t(seconds)
    out = np.zeros(len(x))
    for ratio, level in partials:
        out += np.sin(2 * np.pi * base * ratio * x) * level * np.exp(-x * (1.2 + ratio * 0.7))
    return out


def talisman_feed():
    """Warm stone taking the charge: a low bowl tone with a second one answering."""
    out = chime(196.0, 2.4)
    second = chime(293.7, 2.4) * 0.6
    out[int(SR * 0.22):] += second[: len(out) - int(SR * 0.22)]
    return reverb(fade(out, 0.005, 0.4), 1.6, 0.35)


def talisman_crack():
    """Stone splitting: one dry snap and a little grit."""
    seconds = 0.6
    x = t(seconds)
    snap = bandpass(noise(seconds), 600, 5000) * np.exp(-x / 0.02)
    grit = bandpass(noise(seconds), 1500, 6000) * np.exp(-np.clip(x - 0.05, 0, None) / 0.12) * (x > 0.05) * 0.25 * (rng.random(len(x)) > 0.7)
    return fade(snap + grit + thump(seconds, 220.0, 90.0, 0.05) * 0.5, 0.001, 0.1)


def ward_pulse():
    """Breath in, then out: a swell that snaps into a soft wide boom with a ringing edge."""
    swell = bandpass(noise(0.7), 300, 2200) * np.linspace(0.0, 1.0, int(SR * 0.7)) ** 3 * 0.5
    hit = thump(1.6, 120.0, 45.0, 0.45) + chime(523.3, 1.6) * 0.35
    return reverb(np.concatenate([swell, hit]), 1.5, 0.35)


def clear_air():
    """The mist drawing back: a slow rising rush of air."""
    seconds = 2.2
    n = int(SR * seconds)
    raw = noise(seconds)
    out = np.zeros(n)
    block = 2048
    for start in range(0, n - block, block // 2):
        centre = 300.0 * (14.0 ** (start / n))
        seg = bandpass(raw[start:start + block] * np.hanning(block), centre * 0.7, min(centre * 1.5, SR / 2.3))
        out[start:start + block] += seg
    return reverb(out * envelope(n, 0.8, 0.9), 1.2, 0.3)


def adrenaline():
    """Three hard heartbeats, speeding up."""
    seconds = 1.9
    x = t(seconds)
    out = np.zeros(len(x))
    for at in (0.0, 0.16, 0.62, 0.76, 1.14, 1.26):
        beat = thump(seconds, 95.0, 42.0, 0.09)
        i = int(SR * at)
        out[i:] += beat[: len(out) - i] * (1.0 if int(at * 100) % 2 == 0 else 0.7)
    return fade(lowpass(out, 220), 0.001, 0.2)


def level_up():
    """Two bowl notes, the second a fifth above."""
    out = chime(392.0, 1.8)
    second = chime(587.3, 1.8) * 0.9
    i = int(SR * 0.18)
    out[i:] += second[: len(out) - i]
    return reverb(fade(out, 0.005, 0.3), 1.2, 0.3)


# ------------------------------------------------------------------ recordings
def load(name):
    data, rate = sf.read(os.path.join(RAW, name + ".ogg"))
    if data.ndim > 1:
        data = data.mean(axis=1)
    if rate != SR:
        ratio = Fraction(SR, rate).limit_denominator(1000)
        data = signal.resample_poly(data, ratio.numerator, ratio.denominator)
    return data


def trim(x, threshold=0.03, pad=0.02):
    """Cut silence off both ends."""
    level = np.abs(x) / max(1e-9, np.abs(x).max())
    loud = np.nonzero(level > threshold)[0]
    if len(loud) == 0:
        return x
    return x[max(0, loud[0] - int(SR * pad)): min(len(x), loud[-1] + int(SR * pad))]


def clip(x, start, seconds):
    return x[int(SR * start): int(SR * (start + seconds))]


def slow(x, factor):
    """Play slower and lower, like a tape slowed down."""
    ratio = Fraction(factor).limit_denominator(100)
    return signal.resample_poly(x, ratio.numerator, ratio.denominator)


def recordings():
    whisper = trim(load("hollow_whisper_1"))
    save("hollow_whisper_1", reverb(fade(highpass(whisper, 150), 0.05, 0.3), 1.4, 0.35), 0.8)
    whisper = clip(trim(load("hollow_whisper_2")), 0.5, 4.5)
    save("hollow_whisper_2", reverb(fade(highpass(whisper, 150), 0.2, 0.6), 1.4, 0.35), 0.8)
    save("hollow_breath", reverb(fade(clip(trim(load("hollow_breath")), 0.0, 4.5), 0.1, 0.8), 1.0, 0.25), 0.8)
    for i in (1, 2):
        knock = trim(load(f"hollow_knock_{i}"), 0.05)
        save(f"hollow_knock_{i}", reverb(fade(clip(knock, 0.0, 2.5), 0.002, 0.2), 0.8, 0.2))
    save("skitterer_idle_1", fade(clip(trim(load("skitterer_idle_1")), 0.0, 2.2), 0.01, 0.3))
    save("skitterer_idle_2", fade(clip(trim(load("skitterer_idle_2")), 0.0, 2.0), 0.01, 0.3))
    save("skitterer_hurt", fade(trim(load("skitterer_hurt")), 0.005, 0.15))
    save("stinger_buzz_1", fade(trim(load("stinger_buzz_1")), 0.02, 0.2))
    save("stinger_buzz_2", fade(clip(trim(load("stinger_buzz_2")), 0.0, 2.2), 0.05, 0.4))
    save("stinger_hurt", fade(clip(trim(load("stinger_hurt")), 0.0, 1.3), 0.005, 0.3))
    # One toll of the church bell, slowed to three quarters speed so it is lower and heavier.
    bell = trim(load("dusk_bell"), 0.08)
    save("dusk_bell", reverb(fade(slow(clip(bell, 0.0, 5.5), 4 / 3), 0.005, 2.5), 2.2, 0.35))


# ------------------------------------------------------------------ sounds.json
EVENTS = {
    # event: (files, subtitle)
    "mist.loop": (["mist_loop"], None),
    "mist.horn": (["mist_horn"], "A horn in the distance"),
    "dusk.bell": (["dusk_bell"], "A bell tolls"),
    "hollow.idle": (["hollow_whisper_1", "hollow_whisper_2", "hollow_breath"], "Something whispers"),
    "hollow.knock": (["hollow_knock_1", "hollow_knock_2"], "Knocking"),
    "skitterer.idle": (["skitterer_idle_1", "skitterer_idle_2"], "Skitterer chitters"),
    "skitterer.hurt": (["skitterer_hurt"], "Skitterer shrieks"),
    "stinger.idle": (["stinger_buzz_1", "stinger_buzz_2"], "Wings buzz"),
    "stinger.hurt": (["stinger_hurt"], "Stinger screeches"),
    "leviathan.call": (["strider_call"], "Something calls in the mist"),
    "colossus.call": (["colossus_call"], "Something vast calls"),
    "leviathan.step": (["leviathan_step"], "The ground shakes"),
    "leviathan.crack": (["leviathan_crack"], "Something gives way"),
    "leviathan.crash": (["leviathan_crash"], "Something enormous falls"),
    "talisman.feed": (["talisman_feed"], "Talisman warms"),
    "talisman.crack": (["talisman_crack"], "Talisman cracks"),
    "ability.ward_pulse": (["ward_pulse"], "Ward pulses"),
    "ability.clear_air": (["clear_air"], "The mist draws back"),
    "ability.adrenaline": (["adrenaline"], "Heart pounds"),
    "level_up": (["level_up"], None),
}


def main():
    print("synthesized:")
    save("mist_loop", mist_loop(), 0.5)
    save("mist_horn", mist_horn())
    save("colossus_call", colossus_call())
    save("strider_call", strider_call())
    save("leviathan_step", footfall())
    save("leviathan_crack", crack())
    save("leviathan_crash", crash())
    save("talisman_feed", talisman_feed(), 0.7)
    save("talisman_crack", talisman_crack(), 0.8)
    save("ward_pulse", ward_pulse())
    save("clear_air", clear_air(), 0.7)
    save("adrenaline", adrenaline())
    save("level_up", level_up(), 0.6)
    print("recordings:")
    recordings()

    sounds, lang = {}, {}
    for event, (files, subtitle) in EVENTS.items():
        entry = {"sounds": [f"fromthemist:{name}" for name in files]}
        if subtitle:
            key = "subtitles.fromthemist." + event
            entry["subtitle"] = key
            lang[key] = subtitle
        sounds[event] = entry
    with open(os.path.join(ASSETS, "sounds.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(sounds, f, indent=2)
        f.write("\n")
    with open(os.path.join(HERE, "sound_subtitles.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(lang, f, indent=2)
        f.write("\n")
    print("sounds.json written,", len(EVENTS), "events")


if __name__ == "__main__":
    main()
