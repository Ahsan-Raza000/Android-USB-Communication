# Android-USB-Communication
simple app which shows USB communication between Mobile And Arduino built on android studio using kotlin 
here is the code for arduino:
void setup() {
  Serial.begin(9600); // Start serial communication at 9600 baud rate
  randomSeed(analogRead(0)); // Seed the random number generator
}

void loop() {
  int randomNumber = random(0, 101); // Generate a random number between 0 and 100
  Serial.println(randomNumber); // Send the random number over the serial port
  delay(1000); // Wait for 1 second before generating the next number
}

  note: this is the simple app which shows communication of mobile app with the arduino uno , this app have  connection and diconnect button and recieves data from the arduino. 
