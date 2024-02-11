package pl.edu.agh.mwo.hibernate;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import pl.edu.agh.mwo.hibernate.Util.HibernateUtil;
import pl.edu.agh.mwo.hibernate.models.Album;
import pl.edu.agh.mwo.hibernate.models.Photo;
import pl.edu.agh.mwo.hibernate.models.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Main {

    Session session;

    public static void main(String[] args) {
        Main main = new Main();

        // tu wstaw kod aplikacji

        User user4 = main.getUser("user4");
        User user1 = main.getUser("user1");
        Photo bmw = main.getPhoto("user1", "cars", "bmw");

        main.printDataTree();
        main.close();
    }

    public Main() {
        session = HibernateUtil.getSessionFactory().openSession();
    }

    public String getCurrentDate() {

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }


    public User addUser(String username) {
        org.hibernate.query.Query<User> query = session.createQuery("from User ", User.class);
        List<User> users = query.list();

        boolean userAlreadyExists = users.stream().anyMatch(u -> u.getUsername().equals(username));
        if (userAlreadyExists) {
            System.out.println("User with '" + username + "' username already exists");
            return null;
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setJoinDate(getCurrentDate());

        Transaction transaction = session.beginTransaction();
        session.save(newUser);
        transaction.commit();

        System.out.println("User added successfully");
        return newUser;
    }

    public void removeUser(User user) {
        Transaction transaction = session.beginTransaction();
        for (Photo photo : user.getLikedPhotos()) {
            photo.removeLikedBy(user);
        }
        session.delete(user);
        transaction.commit();
    }

    public User getUser(String username) {
        String hql = "FROM User C WHERE C.username=:username";
        Query<User> query = session.createQuery(hql, User.class);
        query.setParameter("username", username);
        return query.uniqueResult();
    }

    public Album addAlbum(User user, String albumName, String albumDescription) {

        boolean albumAlreadyExists = user.getAlbums().stream().anyMatch(u -> u.getName().equals(albumName));
        if (albumAlreadyExists) {
            System.out.println(user.getUsername() + " already has album with '" + albumName + "' name");
            return null;
        }

        Album newAlbum = new Album();
        newAlbum.setName(albumName);
        newAlbum.setDescription(albumDescription);

        user.addAlbum(newAlbum);

        Transaction transaction = session.beginTransaction();
        session.save(newAlbum);
        transaction.commit();

        System.out.println("Album added successfully");
        return newAlbum;
    }

    public void removeAlbum(Album album) {
        String hql = "SELECT u " +
                "FROM User u " +
                "JOIN u.albums a WHERE a.id = :albumId";
        org.hibernate.query.Query<User> query = session.createQuery(hql, User.class);
        query.setParameter("albumId", album.getId());
        User owner = query.uniqueResult();

        Transaction transaction = session.beginTransaction();
        owner.removeAlbum(album);
        for (Photo photo : album.getPhotos()) {
            for (User user : photo.getLikedBy()) {
                user.getLikedPhotos().remove(photo);
            }
        }
        session.delete(album);
        transaction.commit();
        System.out.println("Album deleted successfully");
    }

    public Album getAlbum(String username, String albumName) {
        String hql = "SELECT a " +
                "FROM User u " +
                "INNER JOIN u.albums a " +
                "WHERE u.username = :username AND a.name = :albumName";
        org.hibernate.query.Query<Album> query = session.createQuery(hql, Album.class);
        query.setParameter("username", username);
        query.setParameter("albumName", albumName);
        return query.uniqueResult();
    }


    public Photo addPhoto(Album album, String photoName) {
        Photo newPhoto = new Photo();
        newPhoto.setName(photoName);
        newPhoto.setDate(getCurrentDate());

        album.addPhoto(newPhoto);

        Transaction transaction = session.beginTransaction();
        session.save(newPhoto);
        transaction.commit();
        System.out.println("Photo added successfully");
        return newPhoto;
    }

    public void removePhoto(Photo photo) {
        Transaction transaction = session.beginTransaction();
        for (User user : photo.getLikedBy()) {
            user.getLikedPhotos().remove(photo);
        }
        session.delete(photo);
        transaction.commit();
        System.out.println("Photo deleted successfully");
    }

    public Photo getPhoto(String username, String albumName, String photoName) {
        String hql = "SELECT p " +
                "FROM User u " +
                "INNER JOIN u.albums a " +
                "INNER JOIN a.photos p " +
                "WHERE u.username = :username AND a.name = :albumName AND p.name = :photoName";
        org.hibernate.query.Query<Photo> query = session.createQuery(hql, Photo.class);
        query.setParameter("username", username);
        query.setParameter("albumName", albumName);
        query.setParameter("photoName", photoName);

        return query.uniqueResult();
    }

    public void likePhoto(User user, Photo photo) {
        String hql = "SELECT u FROM User u " +
                "JOIN u.albums a " +
                "JOIN a.photos p " +
                "WHERE p.id = :photoId";
        org.hibernate.query.Query<User> query = session.createQuery(hql, User.class);
        query.setParameter("photoId", photo.getId());
        User photoOwner = query.uniqueResult();

        if (!user.getFriends().contains(photoOwner)) {
            System.out.println(user.getUsername() + " is not friends with " + photoOwner.getUsername() + ". User can only like photos of people he is friends with");
            return;
        }

        boolean alreadyLiked = user.getLikedPhotos().stream().anyMatch(p -> p.getId() == photo.getId());
        if (alreadyLiked) {
            System.out.println(user.getUsername() + " already liked this photo");
            return;
        }

        user.addLikedPhoto(photo);
        photo.addLikedBy(user);

        Transaction transaction = session.beginTransaction();
        session.save(user);
        session.save(photo);
        transaction.commit();
        System.out.println("Photo liked successfully");
    }

    public void dislikePhoto(User user, Photo photo) {
        user.removeLikedPhoto(photo);
        photo.removeLikedBy(user);

        Transaction transaction = session.beginTransaction();
        session.save(user);
        session.save(photo);
        transaction.commit();
    }

    public void addFriend(User user, User friend) {
        if (user.getId() == friend.getId()) {
            return;
        }
        if (user.getFriends().contains(friend)) {
            return;
        }
        user.addFriend(friend);
        friend.addFriend(user);

        Transaction transaction = session.beginTransaction();
        session.save(user);
        session.save(friend);
        transaction.commit();

        System.out.println("friend added successfully");
    }

    public void removeFriend(User user, User friend) {
        user.removeFriend(friend);
        friend.removeFriend(user);


        Transaction transaction = session.beginTransaction();
        session.save(user);
        session.save(friend);
        transaction.commit();

        System.out.println("friend removed successfully");
    }


    public void printDataTree() {
        org.hibernate.query.Query<User> query = session.createQuery("from User ", User.class);
        List<User> users = query.list();

        for (User user : users) {
            System.out.println(user);

            System.out.println("\tFriend list:");
            if (user.getFriends().size() == 0) {
                System.out.println("\t\tThis user has no friends:");
            } else {
                for (User friend : user.getFriends()) {
                    System.out.println("\t\t" + friend);
                }
            }

            System.out.println("\tLiked photos:");
            if (user.getLikedPhotos().size() == 0) {
                System.out.println("\t\tThis user has no liked photos:");
            } else {
                for (Photo photo : user.getLikedPhotos()) {
                    System.out.println("\t\t" + photo);
                }
            }


            System.out.println("\tAlbums:");
            if (user.getAlbums().size() == 0) {
                System.out.println("\t\tThis user has no albums");
                continue;
            }
            for (Album album : user.getAlbums()) {
                System.out.println("\t\t" + album);
                if (album.getPhotos().size() == 0) {
                    System.out.println("\t\t\tThis album has no photos");
                    continue;
                }
                for (Photo photo : album.getPhotos()) {
                    System.out.println("\t\t\t\t" + photo + " likes: " + photo.getLikedBy().size());
                }
            }
        }
    }


    public void close() {
        session.close();
        HibernateUtil.shutdown();
    }
}
