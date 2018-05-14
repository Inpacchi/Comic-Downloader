# Comic-Downloader
Comic-Downloader takes a URL from the main page of a comic book on readcomics.io and downloads each issue, then converts them to a .cbz format.

# History
After watching Avengers: Infinity War, my interest level in comics spiked up 1000 times (not that it wasn't high already.) The problem is, there is no free way to read comics on mobile devices. For manga, there is, and you can use websites, but there is no actual app that does this. So I wanted a solution where I could read comics on any app I wanted (like Astonishing Comic Reader) and that's where this project was born!

# Technical Overview
As of right now, the application is developed purely in Java and I have no intentions on changing that. To run the code, either import into your IDE or run the .jar file that I will be uploading to the releases page soon. 

# TODO
- Implement cross-platform functionality
- Implement save states and integrate with a database
  - Considering either PostgreSQL or JSON files
- Check pages for integrity instead of deletion
- Implement a GUI
- Test URLs for actual connection status
  - Test URL for validity
- Implement multi-threading for concurrent downloading

# Priorities
 - Clean up, reformat, and comment code
 - Implement multi-threading

# Bugs
None at the moment (and I'd like to keep it that way!)
