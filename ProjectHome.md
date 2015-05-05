DotDash Keyboard is an Android soft keyboard (i.e, an Input Method) for entering text via Morse Code.

## Google Play Page ##

  * [DotDash Market on Google Play](https://play.google.com/store/apps/details?id=net.iowaline.dotdash)

## Setup ##

As an Input Method, DotDash Keyboard doesn't show up in your Applications list. Instead, you'll need to activate it from your phone's settings, and then it will simply appear whenever you invoke the on-screen keyboard. Specific setup instructions may vary depending on your version of Android. For Android 2.3 (which I have) visit the installation page here:
  * [Installation](Installation.md)

## Usage ##

DotDash Keyboard uses a non-standard "untimed" Morse code input method. It has three main buttons: Dot, Dash, and Space (as well as Shift and Delete).

  * Type a letter by entering its series of dots and dashes
  * Press Space to end a letter and move on to the next one
  * Press Space twice to insert a space between words
  * Press Delete to clear the letter in progress, or delete the last letter typed if there is no letter in progress
  * Type ". - . -" to simulate pressing the "Enter" key

## Morse code help ##

While running the keyboard, swipe your finger up off of the keyboard to launch a help screen showing all of the supported Morse code groups. They're also listed in a table here:
  * [SupportedMorseCodeGroups](SupportedMorseCodeGroups.md)

## Why a Morse code keyboard? ##

DotDash Keyboard came out of my frustration with the on-screen keyboard for my LG Optimus P500. It's a fine phone, and I appreciated its low price, but every on-screen keyboard was difficult to use on its 320x480 screen. It seemed like they had all been designed for and tested on high-end machines with much larger screens and finer resolutions. Every time I tried to type, I had numerous typos from hitting the wrong key.

For most people, autocorrection and autocompletion seem to be the features that address this problem, but I never felt satisfied with them. Perhaps it's because I'm a very fast and precise typist on my desktop machine, so I couldn't stand a system that gives me a word other than what I meant 5% or more of the time. Instead, I wanted a keyboard that supported correct input 100% of the time.

I tried two gesture-based keyboards, Swype and Graffiti. These were both excellent (Graffiti especially, since I'd already memorized it when I had a Palm pilot years before), but they had problems with skipping. Occasionally my phone would start lagging and skipping in its response to UI, and when this happened it would skip portions of the path I'd traced onscreen, interpolating it into a diagonal line between the parts it did pick up. Graffiti in particular tended to interpret these diagonal lines as the "Enter" command, causing frequent accidental submissions of things like tweets and IMs.

So, I decided that what I needed was an Input Method with fewer buttons, and no reliance on the CPU processing input events without delay. I considered chording, but didn't want to bother memorizing a whole new system. That's when I hit on Morse code, which I had taught myself after writing up a Morse code tutor program on my Apple IIe (in BASIC) as a child. In order to avoid relying on the CPU's timing, I came up with the scheme of having a separate button for the dot, the dash, and the space between dots, dashes, and words.

I implemented this one long weekend in January 2012, and gradually polished it up. I immediately started using it to test out its viability, and it wound up becoming my primary input method, having all the advantages I had thought it would. (Although it wasn't quite good enough to use without looking at the screen, as my fingers tended to drift off center of the buttons).

I had intended to implement a timing-based version more similar to what ham radio operators use before putting it up on the Android market, but then on April 2 someone sent me a link to GMail's April Fool's Day video [GMail Tap](http://mail.google.com/mail/help/promos/tap/index.html). It was a real "my life is a joke" moment, seeing an app that looked nearly exactly the same as the one I'd come up with, even explained with the same design decisions, put forth as an April Fool's joke! :)

The video made me decide to publish my app sooner than I had originally intended to, in order to try to pick up users genuinely interested in a Morse code keyboard. So, here it is.