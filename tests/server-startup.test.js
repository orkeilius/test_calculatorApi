const http = require("http");

describe("Server startup", () => {
  it("should listen on PORT and execute the callback logging to console if TEST_STARTUP is true", () => {
    // Save original env
    const originalEnv = process.env.TEST_STARTUP;
    process.env.TEST_STARTUP = "true";

    // Isolate module so "server.js" is evaluated fresh
    isolateModules(() => {
      // Mock console.log
      const consoleSpy = spyOn(console, "log").mockImplementation();

      // Mock http.createServer to intercept the listen call
      const mockListen = fn((port, cb) => {
        // Execute the callback immediately to trigger the console.log
        if (cb) cb();
      });

      spyOn(http, "createServer").mockReturnValue({
        listen: mockListen,
      });

      // Require the server file, which will now execute the startup block
      require("../src/server");

      // Verify server starts on port 3000
      expect(mockListen).toHaveBeenCalledWith(3000, expect.any(Function));

      // Verify the log was produced
      expect(consoleSpy).toHaveBeenCalledWith("Serveur démarré sur http://localhost:3000");

      consoleSpy.mockRestore();
    });

    // Restore original env
    process.env.TEST_STARTUP = originalEnv;
  });
});
