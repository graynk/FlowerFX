# FlowerFX
A small library to manage animated switching between panes in JavaFX. I made it for usage in an embedded environment, since DataFX
was performing pretty poorly and afterburner.fx is documented only with videos and seemed too restrictive.

(However, please don't use this if you find this, I don't know what I'm doing, even if it works for me) 

## Dependencies
* compile time dependency for annotations from JetBrains

## Usage
In the `start()` method of your application create a Flower object and pass to the constructor
the stage, FXML of the main controller and FXML of the first desired scene.  

`Flower<Menu> flower = new Flower<>(primaryStage, Main.class.getResource("view/menu/menu.fxml"),
                 Main.class.getResource("view/menu/main.fxml"), Flower.AnimatedTransition.SWIPE);`

The main controller (`Menu` in this case) must implement the interface `Flowable`, which basically means that there have 
to be methods that look somewhat like this:

     @Override
     public void setPane(Node node) {
         mainPane.getChildren().setAll(node);
     }
 
     @Override
     public void addPane(Node node) {
         mainPane.getChildren().add(node);
     }
 
     @Override
     public void removePrevPane() {
         mainPane.getChildren().remove(0);
     }
 
     @Override
     public void setSwapper(Swapper swapper) {
         this.swapper = swapper;
     }
Other controllers must implement the interface `Swappable`, which is just `setSwapper(Swapper swapper)`

Then, in the event handlers for your buttons in the scene (or somewhere else where you want) call the swapper to change 
the scene to the next:
`swapper.changeView(AnalysisMenu.class.getResource("journal/journal.fxml"));`
or to the previous one:
`swapper.fuckGoBack()`

To pass the data between the scenes you can use `addToContext(String key, Object value)`, 
`getFromContext(String key)` and `takeOutOfContext(String key)` methods of the swapper. 