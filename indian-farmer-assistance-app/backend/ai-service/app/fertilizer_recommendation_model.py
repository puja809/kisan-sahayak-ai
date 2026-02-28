import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.preprocessing import LabelEncoder
import joblib
import os

class FertilizerRecommendationModel:
    def __init__(self):
        self.model_n = None
        self.model_p = None
        self.model_k = None
        self.label_encoders = {}
        self.feature_names = ['soil_pH', 'temperature', 'humidity', 'rainfall']
        self.categorical_features = ['crop', 'soil_type', 'season']
        self.target_names = ['N_dosage', 'P_dosage', 'K_dosage']
        
    def train(self, csv_path):
        """Train fertilizer recommendation models from CSV"""
        df = pd.read_csv(csv_path)
        
        X = df[self.feature_names + self.categorical_features].copy()
        y_n = df['N_dosage'].copy()
        y_p = df['P_dosage'].copy()
        y_k = df['K_dosage'].copy()
        
        # Encode categorical features
        for col in self.categorical_features:
            self.label_encoders[col] = LabelEncoder()
            X[col] = self.label_encoders[col].fit_transform(X[col])
        
        # Train separate models for N, P, K dosages
        self.model_n = RandomForestRegressor(
            n_estimators=100,
            max_depth=15,
            random_state=42,
            n_jobs=-1
        )
        self.model_n.fit(X, y_n)
        
        self.model_p = RandomForestRegressor(
            n_estimators=100,
            max_depth=15,
            random_state=42,
            n_jobs=-1
        )
        self.model_p.fit(X, y_p)
        
        self.model_k = RandomForestRegressor(
            n_estimators=100,
            max_depth=15,
            random_state=42,
            n_jobs=-1
        )
        self.model_k.fit(X, y_k)
        
        return self
    
    def predict(self, crop, soil_type, soil_pH, temperature, humidity, rainfall, season):
        """Predict fertilizer dosages"""
        if self.model_n is None or self.model_p is None or self.model_k is None:
            raise ValueError("Models not trained. Call train() first.")
        
        # Encode categorical inputs
        crop_encoded = self.label_encoders['crop'].transform([crop])[0]
        soil_type_encoded = self.label_encoders['soil_type'].transform([soil_type])[0]
        season_encoded = self.label_encoders['season'].transform([season])[0]
        
        X = np.array([[soil_pH, temperature, humidity, rainfall,
                       crop_encoded, soil_type_encoded, season_encoded]])
        
        n_dosage = float(self.model_n.predict(X)[0])
        p_dosage = float(self.model_p.predict(X)[0])
        k_dosage = float(self.model_k.predict(X)[0])
        
        return {
            'N_dosage': round(n_dosage, 2),
            'P_dosage': round(p_dosage, 2),
            'K_dosage': round(k_dosage, 2),
            'total_dosage': round(n_dosage + p_dosage + k_dosage, 2)
        }
    
    def save(self, model_path):
        """Save trained models"""
        os.makedirs(os.path.dirname(model_path), exist_ok=True)
        joblib.dump({
            'model_n': self.model_n,
            'model_p': self.model_p,
            'model_k': self.model_k,
            'label_encoders': self.label_encoders,
            'feature_names': self.feature_names,
            'categorical_features': self.categorical_features
        }, model_path)
    
    def load(self, model_path):
        """Load trained models"""
        data = joblib.load(model_path)
        self.model_n = data['model_n']
        self.model_p = data['model_p']
        self.model_k = data['model_k']
        self.label_encoders = data['label_encoders']
        self.feature_names = data['feature_names']
        self.categorical_features = data['categorical_features']
        return self
