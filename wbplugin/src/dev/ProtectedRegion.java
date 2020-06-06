package dev;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.Listener;

public class ProtectedRegion implements Listener {
	
	public static class ProtectedArea {
		
		public static final int SIZE = 48; // 8 bytes per double, 6 doubles
		private final double minx, miny, minz, maxx, maxy, maxz;
		
		public ProtectedArea(double x1, double y1, double z1, double x2, double y2, double z2) {
			minx = Math.min(x1, x2);
			miny = Math.min(y1, y2);
			minz = Math.min(z1, z2);
			
			maxx = Math.max(x1, x2);
			maxy = Math.max(y1, y2);
			maxz = Math.max(z1, z2);
		}
		
		public ProtectedArea(byte[] bytes) throws BufferUnderflowException {
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			
			minx = buffer.getDouble();
			miny = buffer.getDouble();
			minz = buffer.getDouble();
			
			maxx = buffer.getDouble();
			maxy = buffer.getDouble();
			maxz = buffer.getDouble();
		}
		
		public boolean contains(Location l) {
			return (l.getX() >= minx && l.getX() <= maxx) && (l.getY() >= miny && l.getY() <= maxy) && (l.getZ() >= minz && l.getZ() <= maxz);
		}
		
		public byte[] toBytes() {
			byte[] ret = new byte[ProtectedArea.SIZE];
			ByteBuffer bytes = ByteBuffer.wrap(ret);
			
			bytes.putDouble(minx);
			bytes.putDouble(miny);
			bytes.putDouble(minz);
			
			bytes.putDouble(maxx);
			bytes.putDouble(maxy);
			bytes.putDouble(maxz);
			
			return ret;
		}
		
		@Override
		public String toString() {
			return String.format("[(%f, %f, %f), (%f, %f, %f)]", minx, miny, minz, maxx, maxy, maxz);
		}
	}
	
	
	private static final Path SAVE_PATH = Paths.get("plugins", "wbdata", "protected.rgn");
	// make this thread safe
	public static LinkedList<ProtectedRegion> regions = new LinkedList<ProtectedRegion>();
	
	private List<ProtectedArea> effectiveAreas;
	private ProtectedArea outerArea;
	private boolean verbose;
	
	
	
	private ProtectedRegion(Location point1, Location point2, boolean smart, boolean verbose) {
		effectiveAreas = new ArrayList<ProtectedArea>();
		outerArea = new ProtectedArea(point1.getX(), point1.getY(), point1.getZ(), point2.getX(), point2.getY(), point2.getZ());
		if (!smart) {
			effectiveAreas.add(new ProtectedArea(point1.getX(), point1.getY(), point1.getZ(), point2.getX(), point2.getY(), point2.getZ()));
		}
		else {
			smartInitialization();
		}
		this.verbose = verbose;
	}
	
	private ProtectedRegion(ProtectedArea outerArea, List<ProtectedArea> effectiveAreas, boolean verbose) {
		this.outerArea = outerArea;
		this.effectiveAreas = effectiveAreas;
		this.verbose = verbose;
	}
	
	
	private void smartInitialization() {
		
	}
	
	
	public boolean contains(Location l) {
		if (l == null) return false;
		
		for (ProtectedArea area : effectiveAreas)
			if (area.contains(l)) return true;
			
		return false;
	}
	
	public boolean isVerbose() {
		return verbose;
	}
	
	
	public void delete() {
		regions.remove(this);
	}
	
	
	public byte[] toBytes() {
		byte[] ret = new byte[(effectiveAreas.size() + 1) * ProtectedArea.SIZE + 5];
		ByteBuffer bytes = ByteBuffer.wrap(ret);
		
		bytes.put((byte)(verbose ? 1 : 0));
		bytes.put(outerArea.toBytes());
		bytes.putInt(effectiveAreas.size());
		for (ProtectedArea area : effectiveAreas) {
			bytes.put(area.toBytes());
		}
		
		return ret;
	}
	
	@Override
	public String toString() {
		return String.format("%s : %s", outerArea.toString(), verbose ? "true" : "false");
	}
	
	
	
	public static void create(Location point1, Location point2, boolean smart, boolean verbose) {
		regions.add(new ProtectedRegion(point1, point2, smart, verbose));
	}
	
	
	
	public static void load() {
		if (!Files.exists(SAVE_PATH)) {
			System.out.println("[WBPlugin] No region data file found.");
			return;
		}
		
		FileInputStream reader = null;
		try {
			reader = new FileInputStream(SAVE_PATH.toFile());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		
		int next;
		boolean verbose;
		ProtectedArea outerArea;
		List<ProtectedArea> effectiveAreas = new ArrayList<ProtectedArea>();
		int numAreas;
		System.out.println("[WBPlugin] Loading protected region data from file...");
		try {
			while ((next = reader.read()) != -1) {
				verbose = (next & 0xFF) != 0 ? true : false;
				try {
					byte[] bytes = reader.readNBytes(ProtectedArea.SIZE + 4);
					ByteBuffer buffer = ByteBuffer.wrap(bytes, ProtectedArea.SIZE, 4);
					outerArea = new ProtectedArea(bytes);
					numAreas = buffer.getInt();
					
					for (int i = 0; i < numAreas; ++i) {
						bytes = reader.readNBytes(ProtectedArea.SIZE);
						effectiveAreas.add(new ProtectedArea(bytes));
					}
				} catch (BufferUnderflowException bu) {
					System.out.println("[WBPlugin] ERROR: data is corrupted, deleting file.");
					try {
						Files.delete(SAVE_PATH);
					} catch (FileSystemException fs) {
						System.out.println("Unable to delete file.");
					} catch (IOException io) {
						io.printStackTrace();
					}
					break;
				}
				
				regions.add(new ProtectedRegion(outerArea, effectiveAreas, verbose));
			}
		} catch (IOException io) {
			io.printStackTrace();
		}
		System.out.println(String.format("[WBPlugin] Loaded %d region(s).", regions.size()));
	}
	
	
	public static void save() {
		if (regions.size() == 0) return;
		
		if (Files.exists(SAVE_PATH)) {
			try {
				PrintWriter writer = new PrintWriter(SAVE_PATH.toFile());
				writer.write("");
				writer.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		else {
			try {
				if (!Files.exists(SAVE_PATH.getParent()))
					Files.createDirectories(SAVE_PATH.getParent());
				Files.createFile(SAVE_PATH);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// this may not be very efficient since the file is opened and closed for each write call
		// consider using a buffered writer or something with more control
		for (ProtectedRegion region : regions) {
			try {
				Files.write(SAVE_PATH, region.toBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}