import os
from PIL import Image

def resize_images(input_folder, target_width, target_height):
    """
    Resizes all PNGs in a folder to specific dimensions and saves them
    to a 'resized' subfolder.
    """
    
    # 1. Define the output subfolder path
    output_folder = os.path.join(input_folder, "../", "resized")
    
    # 2. Create the output folder if it doesn't exist
    if not os.path.exists(output_folder):
        os.makedirs(output_folder)
        print(f"Created output folder: {output_folder}")
    else:
        print(f"Saving to existing folder: {output_folder}")

    # 3. List all files in the input folder
    files = os.listdir(input_folder)
    
    count = 0
    
    print(f"Starting resize to {target_width}x{target_height} pixels...")

    for filename in files:
        # 4. Process only .png files (case insensitive)
        if filename.lower().endswith(".png"):
            input_path = os.path.join(input_folder, filename)
            output_path = os.path.join(output_folder, filename)

            try:
                with Image.open(input_path) as img:
                    # 5. Resize the image
                    # Image.Resampling.LANCZOS is a high-quality downsampling filter
                    resized_img = img.resize((target_width, target_height), Image.Resampling.LANCZOS)
                    
                    # 6. Save the image
                    resized_img.save(output_path)
                    print(f"Resized: {filename}")
                    count += 1
            except Exception as e:
                print(f"Failed to process {filename}: {e}")

    print(f"--- Processing Complete. {count} images resized. ---")

# --- CONFIGURATION ---
# Replace these values with your specific needs
FOLDER_PATH = "unresized" # Use 'r' for raw string on Windows
NEW_WIDTH = 1440 #  × 
NEW_HEIGHT = 900

# Run the function
if __name__ == "__main__":
    # check if the folder path is valid before running
    if os.path.isdir(FOLDER_PATH):
        resize_images(FOLDER_PATH, NEW_WIDTH, NEW_HEIGHT)
    else:
        print("Error: The specified folder path does not exist.")