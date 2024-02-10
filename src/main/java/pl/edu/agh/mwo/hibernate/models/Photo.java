package pl.edu.agh.mwo.hibernate.models;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "photos")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Column
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    @Column
    private String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @ManyToMany(mappedBy = "likedPhotos", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Set<User> likedBy = new HashSet<>();

    public Set<User> getLikedBy() {
        return likedBy;
    }

    public void addLikedBy(User user){
        likedBy.add(user);
    }

    public void removeLikedBy(User user){
        likedBy.remove(user);
    }


    @Override
    public String toString() {
        return "Photo { " + name + ": " + date + " }";
    }

}
