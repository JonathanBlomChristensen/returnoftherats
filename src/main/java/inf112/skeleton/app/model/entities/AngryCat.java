package inf112.skeleton.app.model.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

public class AngryCat extends Cat {

    public static Texture texture = new Texture(Gdx.files.internal("angryCat.png"));

    public AngryCat() {
        super(5, 1 ,150, texture);
    }
    
}
