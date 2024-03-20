package inf112.skeleton.app.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import inf112.skeleton.app.model.catmenu.CatMenu;
import inf112.skeleton.app.model.entities.Projectile;
import inf112.skeleton.app.model.entities.cat.BasicCat;
import inf112.skeleton.app.model.entities.cat.Cat;
import inf112.skeleton.app.model.entities.cat.ShotgunCat;
import inf112.skeleton.app.model.entities.cat.FreezeCat;
import inf112.skeleton.app.model.entities.rat.BasicRat;
import inf112.skeleton.app.model.entities.rat.Rat;
import inf112.skeleton.app.model.entities.rat.Rat.Direction;
import inf112.skeleton.app.model.entities.rat.RatFactory;


public class SkadedyrModel implements ISkadedyrModel {
    private ArrayList<Cat> cats = new ArrayList<>();
    private ArrayList<Rat> aliveRats = new ArrayList<>();
    private ArrayList<Projectile> projectiles = new ArrayList<>();
    private RatFactory ratFactory = new RatFactory();
    private int lives = 5;
    private int money = 1000;
    private int points = 0;
    private int level = 0;
    private int ratsSpawned;
    private boolean isPaused = true;
    private CatMenu catMenu;
    private float roundOverDelay = 0f;
    private final float DELAY_DURATION = 0.5f; 
    
    public SkadedyrModel() {
        this.cats = new ArrayList<>();
        this.catMenu = new CatMenu();
    }
    
    public void clockTick() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        updateCatAnimations(deltaTime);
        handleUserInput();
        moveRats(); 
        attackRat();
        rotater();
        //updateProjectiles(deltaTime);
        roundHandler(deltaTime);
        aliveRats = ratFactory.updateRatFactory(deltaTime,level); 
    }

    private void roundHandler(float deltaTime){
        if(isRoundOver()){
            roundOverDelay += deltaTime;
            if(roundOverDelay >= DELAY_DURATION){
                roundOver(deltaTime);
                roundOverDelay = 0f; 
            }
        } else {
            roundOverDelay = 0f;
        }
    }

    private void roundOver(float deltaTime) {
        if (isRoundOver()) {
            nextWaveText();
            level++;
            ratFactory.updateRatFactory(deltaTime, level);
            isPaused = true;
        }
    }
    
    public boolean isRoundOver() {
        int killedRats = 0;
        for (Rat rat : aliveRats) {
            if (rat.isKilled() || rat.isOut()) {
                killedRats++;
            }
            if (killedRats == ratFactory.calculateRatsForRound(level)) {
                return true;
            }
        }
        return false;
    }

    //theo
    public String nextWaveText() {//Kanskje også animasjon?
        //if (isRoundOver()) {
            return "Round over. Press P to continue.";
        //}
        //return "";
    }
    

    private void updateCatAnimations(float deltaTime) {
        for (Cat cat : cats) {
            cat.updateAnimation(deltaTime);
        }
    }

    private void handleUserInput() {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.input.getY();
        Vector2 mouse = new Vector2(mouseX, 842-mouseY);
        
        if (Gdx.input.isTouched() && mouseX < 832) {
            newCat(mouseX, 842 - mouseY);
        }
        if (Gdx.input.isTouched()){
            catMenu.selector(mouse);
        }
    }

    @Override
    public void moveRats() {
        for (Rat rat : aliveRats) {
            rat.move();
        }
    }
  

    @Override
    public void addCat(Cat cat) {
        cats.add(cat);
    }

    public void setPause() {
        isPaused = !isPaused;
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void addRat(Rat rat) {
        aliveRats.add(rat);
    }

    @Override
    public ArrayList<Cat> getCats() {
        return cats;
    }

    @Override
    public ArrayList<Rat> getRats() {
        return aliveRats;
    }


    // public void gameOver() {
    //     GameStateManager.set(new GameOverState(null));

    //     //Gdx.app.exit(); // jacob skjerm
    // }

    public int getRatsSpawned() {
        return ratsSpawned;
    }

    public int getMoney() {
        return money;
    }

    public int getLevel() {
        return level;
    }

    public int getPoints() {
        return points;
    }


    public void rotater() {
        HashMap<Cat, LinkedList<Rat>> attackMap = attackQueueForEachCat();
        for (Cat cat : cats) {
            LinkedList<Rat> attackableRats = attackMap.get(cat);
            if (!attackableRats.isEmpty()) {
                Rat firstRat = attackableRats.getFirst();
                cat.setRotationToward(firstRat);
            } 
            cat.rotateImage();
        }
    }
    
    

    /**
     * Returns the amount of lives the player has left
     * @return lives
     */
    public int getLives() {
        for (Rat rat : aliveRats) {
            if (rat.getDirection() == Direction.OUT) {
                aliveRats.remove(rat);
                return lives--;
            }
        }
        if (lives <= 0) {
           // gameOver();
        }
        return lives;
    }

    /**
     * Returns a hashmap with cats as keys and a linkedlist of rats as values
     * @return
     */

    public HashMap<Cat, LinkedList<Rat>> attackQueueForEachCat() {
        HashMap<Cat, LinkedList<Rat>> attackMap = new HashMap<>();
        for (Cat cat : cats) {
            LinkedList<Rat> attackableRats = new LinkedList<>();
            for (Rat rat : aliveRats) {
                if (cat.withinRange(rat)) {
                    attackableRats.addLast(rat);
                }
            }
            attackMap.put(cat, attackableRats);
        }
        return attackMap;
    }


    public void attackRat() {
        HashMap<Cat, LinkedList<Rat>> attackMap = attackQueueForEachCat();
        for (Cat cat : cats) {
            cat.updateAttackTimer(Gdx.graphics.getDeltaTime());
            LinkedList<Rat> attackableRats = attackMap.get(cat);
            if (cat.canAttack() && !attackableRats.isEmpty()) {
                projectiles.addAll(cat.attack(attackableRats));
                cat.resetAttackTimer();
            }
            for (Rat rat : attackableRats) {
                if (rat.isKilled()) {
                    //hvorfor får man så mye?
                    money += rat.getBounty();
                    points += rat.getPoints();
                }
            }
        }
    }
    
    private void updateProjectiles(float dt) {
        HashMap<Cat, LinkedList<Rat>> attackMap = attackQueueForEachCat();
        for (Cat cat : cats) {
            LinkedList<Rat> attackableRats = attackMap.get(cat);
            if (!attackableRats.isEmpty()) {
                for (Projectile projectile : projectiles) {
                    projectile.update(dt,attackableRats.getFirst(),cat);
                    projectile.pointImageAtRat(attackableRats.getFirst(),cat);
                }
            }
        }
    }

    public ArrayList<Projectile> getProjectiles() {
        return projectiles;
    }
    
    
    private void unfreezeRats() {
        for (Rat rat : aliveRats) {
            if (rat.isFrozen()) {
                rat.unfreeze();
            }
        }
    }

    public void newCat(int mouseX, int mouseY) {
        Cat cat = catMenu.getSelectedCat();
        if (cat instanceof BasicCat){
            cat = new BasicCat();
            money -= cat.getCost();
        }
        else if (cat instanceof ShotgunCat){
            cat = new ShotgunCat();
            money -= cat.getCost();
        }
        else if (cat instanceof FreezeCat){
            cat = new FreezeCat();
            money -= cat.getCost();
        }
        cat.setPos(mouseX, mouseY);
        addCat(cat);
    }

    public CatMenu getBuyMenu() {
        return catMenu;
    }
}


