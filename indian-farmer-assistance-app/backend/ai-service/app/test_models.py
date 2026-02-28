"""
Test script to verify trained models work correctly
"""
import os
from crop_recommendation_model import CropRecommendationModel
from crop_rotation_model import CropRotationModel
from fertilizer_recommendation_model import FertilizerRecommendationModel

def test_models():
    """Test loading and using trained models"""
    
    base_path = os.path.dirname(os.path.abspath(__file__))
    models_dir = os.path.join(base_path, 'models')
    
    print("=" * 60)
    print("ML MODEL TESTING")
    print("=" * 60)
    
    # Test crop recommendation model
    print("\n1. Testing Crop Recommendation Model")
    print("-" * 60)
    try:
        crop_reco = CropRecommendationModel()
        crop_reco.load(os.path.join(models_dir, 'crop_recommendation_model.pkl'))
        print("✓ Model loaded successfully")
        
        # Test prediction
        pred = crop_reco.predict(
            N=100, P=50, K=50,
            temperature=26.5, humidity=78,
            pH=6.5, rainfall=210
        )
        print(f"✓ Prediction successful")
        print(f"  Recommended crop: {pred['crop']}")
        print(f"  Confidence: {pred['confidence']:.2%}")
        print(f"  Top 3 alternatives:")
        sorted_probs = sorted(pred['probabilities'].items(), key=lambda x: x[1], reverse=True)
        for i, (crop, prob) in enumerate(sorted_probs[:3], 1):
            print(f"    {i}. {crop}: {prob:.2%}")
    except Exception as e:
        print(f"✗ Error: {e}")
        return False
    
    # Test crop rotation model
    print("\n2. Testing Crop Rotation Model")
    print("-" * 60)
    try:
        crop_rotation = CropRotationModel()
        crop_rotation.load(os.path.join(models_dir, 'crop_rotation_model.pkl'))
        print("✓ Model loaded successfully")
        
        # Test prediction
        pred = crop_rotation.predict(
            previous_crop='Wheat',
            soil_pH=7.2,
            soil_type='loamy',
            temperature=25.3,
            humidity=82.58,
            rainfall=118.95,
            season='Kharif'
        )
        print(f"✓ Prediction successful")
        print(f"  Recommended next crop: {pred['recommended_next_crop']}")
        print(f"  Confidence: {pred['confidence']:.2%}")
        print(f"  Top 3 alternatives:")
        sorted_probs = sorted(pred['probabilities'].items(), key=lambda x: x[1], reverse=True)
        for i, (crop, prob) in enumerate(sorted_probs[:3], 1):
            print(f"    {i}. {crop}: {prob:.2%}")
    except Exception as e:
        print(f"✗ Error: {e}")
        return False
    
    # Test fertilizer recommendation model
    print("\n3. Testing Fertilizer Recommendation Model")
    print("-" * 60)
    try:
        fertilizer = FertilizerRecommendationModel()
        fertilizer.load(os.path.join(models_dir, 'fertilizer_recommendation_model.pkl'))
        print("✓ Model loaded successfully")
        
        # Test prediction
        pred = fertilizer.predict(
            crop='Rice',
            soil_type='loamy',
            soil_pH=6.5,
            temperature=26.5,
            humidity=78,
            rainfall=210,
            season='Kharif'
        )
        print(f"✓ Prediction successful")
        print(f"  N dosage: {pred['N_dosage']} kg/ha")
        print(f"  P dosage: {pred['P_dosage']} kg/ha")
        print(f"  K dosage: {pred['K_dosage']} kg/ha")
        print(f"  Total dosage: {pred['total_dosage']} kg/ha")
    except Exception as e:
        print(f"✗ Error: {e}")
        return False
    
    print("\n" + "=" * 60)
    print("✓ ALL TESTS PASSED!")
    print("=" * 60)
    return True

if __name__ == '__main__':
    success = test_models()
    exit(0 if success else 1)
