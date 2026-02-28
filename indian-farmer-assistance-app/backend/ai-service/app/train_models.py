"""
Script to train and save crop recommendation, rotation, and fertilizer models
Run this once to generate the model files
"""
import os
from crop_recommendation_model import CropRecommendationModel
from crop_rotation_model import CropRotationModel
from fertilizer_recommendation_model import FertilizerRecommendationModel

def train_and_save_models():
    """Train all models and save them"""
    
    # Paths
    base_path = os.path.dirname(os.path.abspath(__file__))
    models_dir = os.path.join(base_path, 'models')
    os.makedirs(models_dir, exist_ok=True)
    
    # Dataset paths - go up to project root then to documents
    project_root = os.path.abspath(os.path.join(base_path, '../../../..'))
    crop_reco_csv = os.path.join(project_root, 'documents/crop_reco_weatherapi_kaegro.csv')
    crop_rotation_csv = os.path.join(project_root, 'documents/enhanced_crop_rotation_dataset.csv')
    fertilizer_csv = os.path.join(project_root, 'documents/fertilizer_recommendation_dataset.csv')
    
    print("Training Crop Recommendation Model...")
    crop_reco_model = CropRecommendationModel()
    crop_reco_model.train(crop_reco_csv)
    crop_reco_model.save(os.path.join(models_dir, 'crop_recommendation_model.pkl'))
    print(f"✓ Saved: {os.path.join(models_dir, 'crop_recommendation_model.pkl')}")
    
    print("\nTraining Crop Rotation Model...")
    crop_rotation_model = CropRotationModel()
    crop_rotation_model.train(crop_rotation_csv)
    crop_rotation_model.save(os.path.join(models_dir, 'crop_rotation_model.pkl'))
    print(f"✓ Saved: {os.path.join(models_dir, 'crop_rotation_model.pkl')}")
    
    print("\nTraining Fertilizer Recommendation Model...")
    fertilizer_model = FertilizerRecommendationModel()
    fertilizer_model.train(fertilizer_csv)
    fertilizer_model.save(os.path.join(models_dir, 'fertilizer_recommendation_model.pkl'))
    print(f"✓ Saved: {os.path.join(models_dir, 'fertilizer_recommendation_model.pkl')}")
    
    print("\n✓ All models trained and saved successfully!")

if __name__ == '__main__':
    train_and_save_models()
